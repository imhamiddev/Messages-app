package dev.imhamid.messages.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dev.imhamid.messages.SettingsStore
import dev.imhamid.messages.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class Repository(
    private val ctx: Context,
    val db: AppDatabase,
    val contacts: ContactsRepo,
    val settings: SettingsStore
) {
    val messages get() = db.messages()
    val prefs get() = db.prefs()
    val blocked get() = db.blocked()
    val scheduled get() = db.scheduled()

    /** Key of the chat currently on screen, so incoming messages there don't notify. */
    @Volatile
    var activeKey: String? = null

    private val lock = Mutex()
    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing
    private val _imported = MutableStateFlow(0)
    val imported: StateFlow<Int> = _imported

    fun isDefaultSms(): Boolean = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName

    fun canReadSms(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    // ---------- import from the system SMS store ----------

    /** Copies every SMS we do not know yet from the phone's SMS store. Returns how many were added. */
    suspend fun importFromProvider(): Int = withContext(Dispatchers.IO) {
        if (!canReadSms()) return@withContext 0
        lock.withLock {
            _importing.value = true
            _imported.value = 0
            var added = 0
            try {
                val known = messages.knownProviderIds().toHashSet()
                val blockedKeys = blocked.keys().toHashSet()
                val batch = ArrayList<MessageEntity>(400)
                val cols = arrayOf(
                    Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE,
                    Telephony.Sms.TYPE, Telephony.Sms.READ, Telephony.Sms.SUBSCRIPTION_ID
                )
                ctx.contentResolver.query(Telephony.Sms.CONTENT_URI, cols, null, null, "date ASC")?.use { c ->
                    while (c.moveToNext()) {
                        val pid = c.getLong(0)
                        if (pid in known) continue
                        val type = when (c.getInt(4)) {
                            Telephony.Sms.MESSAGE_TYPE_INBOX -> TYPE_INBOX
                            Telephony.Sms.MESSAGE_TYPE_SENT -> TYPE_SENT
                            Telephony.Sms.MESSAGE_TYPE_FAILED -> TYPE_FAILED
                            Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> TYPE_SENDING
                            else -> continue // drafts
                        }
                        val address = (c.getString(1) ?: "").trim()
                        val key = PhoneUtils.key(address)
                        batch += MessageEntity(
                            providerId = pid,
                            threadKey = key,
                            address = address,
                            body = c.getString(2) ?: "",
                            date = c.getLong(3),
                            type = type,
                            read = c.getInt(5) != 0 || type != TYPE_INBOX,
                            subId = c.getInt(6),
                            blocked = key in blockedKeys
                        )
                        if (batch.size >= 400) {
                            messages.insertAll(batch.toList())
                            added += batch.size
                            _imported.value = added
                            batch.clear()
                        }
                    }
                }
                if (batch.isNotEmpty()) {
                    messages.insertAll(batch.toList())
                    added += batch.size
                    _imported.value = added
                }
            } finally {
                _importing.value = false
            }
            added
        }
    }

    // ---------- system SMS provider helpers (only when we are the default app) ----------

    private fun providerInsert(address: String, body: String, date: Long, type: Int, read: Boolean, subId: Int): Long? {
        if (!isDefaultSms()) return null
        return runCatching {
            val v = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, date)
                put(Telephony.Sms.TYPE, type)
                put(Telephony.Sms.READ, if (read) 1 else 0)
                put(Telephony.Sms.SEEN, if (read) 1 else 0)
                if (subId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
            }
            ctx.contentResolver.insert(Telephony.Sms.CONTENT_URI, v)?.let { ContentUris.parseId(it) }
        }.getOrNull()
    }

    private fun providerDelete(providerId: Long) {
        runCatching {
            ctx.contentResolver.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, providerId), null, null)
        }
    }

    // ---------- incoming / outgoing ----------

    /** Stores a received SMS. Returns the stored message, or null when the sender is blocked. */
    suspend fun onIncoming(address: String, body: String, subId: Int): MessageEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            val now = System.currentTimeMillis()
            val key = PhoneUtils.key(address)
            val isBlocked = blocked.count(key) > 0
            val pid = providerInsert(address, body, now, Telephony.Sms.MESSAGE_TYPE_INBOX, false, subId)
            val m = MessageEntity(
                providerId = pid, threadKey = key, address = address, body = body, date = now,
                type = TYPE_INBOX, read = false, subId = subId, blocked = isBlocked
            )
            val id = messages.insert(m)
            if (isBlocked) null else m.copy(id = id)
        }
    }

    /** Creates the outgoing row (status "sending"). */
    suspend fun insertOutgoing(address: String, body: String, subId: Int): MessageEntity = withContext(Dispatchers.IO) {
        lock.withLock {
            val now = System.currentTimeMillis()
            val key = PhoneUtils.key(address)
            val pid = providerInsert(address, body, now, Telephony.Sms.MESSAGE_TYPE_SENT, true, subId)
            val m = MessageEntity(
                providerId = pid, threadKey = key, address = address, body = body, date = now,
                type = TYPE_SENDING, read = true, subId = subId
            )
            m.copy(id = messages.insert(m))
        }
    }

    suspend fun setStatus(id: Long, type: Int) = withContext(Dispatchers.IO) { messages.setType(id, type) }

    suspend fun setStatusIfSending(id: Long, type: Int) = withContext(Dispatchers.IO) { messages.setTypeIfSending(id, type) }

    suspend fun markRead(key: String) = withContext(Dispatchers.IO) {
        if (isDefaultSms()) {
            messages.unreadProviderIds(key).forEach { pid ->
                runCatching {
                    val v = ContentValues().apply { put(Telephony.Sms.READ, 1); put(Telephony.Sms.SEEN, 1) }
                    ctx.contentResolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, pid), v, null, null)
                }
            }
        }
        messages.markRead(key)
    }

    // ---------- trash ----------

    suspend fun trashMessages(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) messages.trashIds(ids, System.currentTimeMillis())
    }

    suspend fun trashThreads(keys: List<String>) = withContext(Dispatchers.IO) {
        if (keys.isNotEmpty()) messages.trashThreads(keys, System.currentTimeMillis())
    }

    suspend fun restore(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) messages.restore(ids)
    }

    /** Permanently removes messages. Rows we cannot delete from the system store are hidden instead. */
    suspend fun deleteForever(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val rows = messages.byIds(ids)
        val default = isDefaultSms()
        val hide = ArrayList<Long>()
        val remove = ArrayList<Long>()
        for (m in rows) {
            if (m.providerId != null && !default) hide += m.id
            else {
                m.providerId?.let { providerDelete(it) }
                remove += m.id
            }
        }
        if (remove.isNotEmpty()) messages.deleteRows(remove)
        if (hide.isNotEmpty()) messages.trashIds(hide, HIDDEN)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        deleteForever(messages.allTrash().map { it.id })
    }

    /** Trash is emptied automatically after 30 days. */
    suspend fun purgeExpiredTrash() = withContext(Dispatchers.IO) {
        val before = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        deleteForever(messages.expiredTrash(before).map { it.id })
    }

    // ---------- pins, SIM per chat ----------

    suspend fun setPinned(keys: List<String>, pinned: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        for (k in keys) {
            val p = prefs.get(k) ?: ChatPref(k)
            prefs.upsert(p.copy(pinned = pinned, pinnedAt = if (pinned) now else 0))
        }
    }

    /** subId == null clears the per-chat choice (falls back to the default SIM). */
    suspend fun setChatSim(key: String, subId: Int?) = withContext(Dispatchers.IO) {
        val p = prefs.get(key) ?: ChatPref(key)
        prefs.upsert(p.copy(simSubId = subId))
    }

    suspend fun chatSim(key: String): Int? = withContext(Dispatchers.IO) { prefs.get(key)?.simSubId }

    // ---------- blocking ----------

    suspend fun block(key: String, address: String) = withContext(Dispatchers.IO) {
        blocked.add(BlockedEntity(key, address, System.currentTimeMillis()))
        messages.setBlocked(key, true)
    }

    suspend fun unblock(key: String) = withContext(Dispatchers.IO) {
        blocked.remove(key)
        messages.setBlocked(key, false)
    }

    // ---------- scheduled messages ----------

    suspend fun addScheduled(address: String, body: String, sendAt: Long, subId: Int): ScheduledEntity =
        withContext(Dispatchers.IO) {
            val s = ScheduledEntity(
                threadKey = PhoneUtils.key(address), address = address, body = body,
                sendAt = sendAt, subId = subId, createdAt = System.currentTimeMillis()
            )
            s.copy(id = scheduled.insert(s))
        }
}
