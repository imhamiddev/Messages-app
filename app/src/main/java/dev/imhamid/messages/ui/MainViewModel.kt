package dev.imhamid.messages.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.imhamid.messages.App
import dev.imhamid.messages.data.BlockedEntity
import dev.imhamid.messages.data.ChatPref
import dev.imhamid.messages.data.Contact
import dev.imhamid.messages.data.MessageEntity
import dev.imhamid.messages.data.ScheduledEntity
import dev.imhamid.messages.sms.Notifier
import dev.imhamid.messages.sms.Scheduler
import dev.imhamid.messages.sms.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ThreadItem(
    val key: String,
    val address: String,
    val name: String?,
    val last: MessageEntity,
    val unread: Int,
    val pinned: Boolean,
    val pinnedAt: Long
) {
    val title: String get() = name ?: address
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    val repo = app.repo
    val settings = app.settings

    val calendar = settings.calendar
    val theme = settings.theme
    val defaultSim = settings.defaultSim
    val bubble = settings.bubble

    /** null until the first database result arrives, so the UI doesn't flash an empty state. */
    val threads: StateFlow<List<ThreadItem>?> = combine(
        repo.messages.observeThreads(),
        repo.messages.observeUnread(),
        repo.prefs.observeAll()
    ) { last, unread, prefs ->
        val unreadMap = unread.associate { it.threadKey to it.cnt }
        val prefMap = prefs.associateBy { it.threadKey }
        last.map { m ->
            val p = prefMap[m.threadKey]
            ThreadItem(
                key = m.threadKey, address = m.address, name = repo.contacts.nameFor(m.address),
                last = m, unread = unreadMap[m.threadKey] ?: 0,
                pinned = p?.pinned == true, pinnedAt = p?.pinnedAt ?: 0L
            )
        }.sortedWith(
            compareByDescending<ThreadItem> { it.pinned }
                .thenByDescending { if (it.pinned) it.pinnedAt else 0L }
                .thenByDescending { it.last.date }
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trash: Flow<List<MessageEntity>> = repo.messages.observeTrash()
    val blockedList: Flow<List<BlockedEntity>> = repo.blocked.observeAll()
    val blockedKeys: Flow<Set<String>> = repo.blocked.observeAll().map { l -> l.map { it.threadKey }.toSet() }
    val scheduledList: Flow<List<ScheduledEntity>> = repo.scheduled.observeAll()

    fun chat(key: String): Flow<List<MessageEntity>> = repo.messages.observeThread(key)
    fun chatPref(key: String): Flow<ChatPref?> = repo.prefs.observe(key)

    suspend fun displayName(address: String): String? =
        withContext(Dispatchers.IO) { repo.contacts.nameFor(address) }

    suspend fun searchContacts(q: String): List<Contact> = repo.contacts.search(q)

    fun runImport() {
        viewModelScope.launch { repo.importFromProvider() }
    }

    fun send(address: String, body: String, subId: Int) {
        viewModelScope.launch { SmsSender.send(getApplication(), address, body, subId) }
    }

    fun resend(m: MessageEntity, subId: Int) {
        viewModelScope.launch {
            repo.deleteForever(listOf(m.id))
            SmsSender.send(getApplication(), m.address, m.body, subId)
        }
    }

    fun markRead(key: String) {
        viewModelScope.launch {
            repo.markRead(key)
            Notifier.cancel(getApplication(), key)
        }
    }

    fun pin(keys: List<String>, pinned: Boolean) {
        viewModelScope.launch { repo.setPinned(keys, pinned) }
    }

    fun trashThreads(keys: List<String>) {
        viewModelScope.launch { repo.trashThreads(keys) }
    }

    fun trashMessages(ids: List<Long>) {
        viewModelScope.launch { repo.trashMessages(ids) }
    }

    fun restore(ids: List<Long>) {
        viewModelScope.launch { repo.restore(ids) }
    }

    fun deleteForever(ids: List<Long>) {
        viewModelScope.launch { repo.deleteForever(ids) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }

    fun block(key: String, address: String) {
        viewModelScope.launch { repo.block(key, address) }
    }

    fun unblock(key: String) {
        viewModelScope.launch { repo.unblock(key) }
    }

    fun setChatSim(key: String, subId: Int?) {
        viewModelScope.launch { repo.setChatSim(key, subId) }
    }

    fun schedule(address: String, body: String, at: Long, subId: Int) {
        viewModelScope.launch { Scheduler.create(getApplication(), address, body, at, subId) }
    }

    fun removeScheduled(id: Long) {
        viewModelScope.launch { Scheduler.remove(getApplication(), id) }
    }
}
