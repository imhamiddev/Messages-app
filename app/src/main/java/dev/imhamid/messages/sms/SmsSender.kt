package dev.imhamid.messages.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import dev.imhamid.messages.app
import dev.imhamid.messages.data.TYPE_FAILED
import dev.imhamid.messages.receivers.SmsStatusReceiver
import dev.imhamid.messages.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsSender {
    const val ACTION_SENT = "dev.imhamid.messages.SMS_SENT"
    const val EXTRA_ID = "id"
    const val EXTRA_LAST = "last"

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun manager(ctx: Context, subId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= 31) {
            val sm = ctx.getSystemService(SmsManager::class.java)
            if (subId >= 0) sm.createForSubscriptionId(subId) else sm
        } else {
            @Suppress("DEPRECATION")
            if (subId >= 0) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }
    }

    /** Chat choice -> app default -> system default; drops ids of SIMs that are no longer inserted. */
    fun resolveSub(ctx: Context, requested: Int): Int {
        val wanted = if (requested >= 0) requested else ctx.app.settings.defaultSim.value
        val sims = SimManager.list(ctx)
        if (wanted < 0) {
            // System default: resolve it to the actual SIM so the message can show which one it used.
            if (sims.isEmpty()) return -1
            val sys = if (sims.size > 1) android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId() else sims[0].subId
            return if (sims.any { it.subId == sys }) sys else -1
        }
        return if (sims.isEmpty() || sims.any { it.subId == wanted }) wanted else -1
    }

    /** Stores the message and hands it to the radio. Returns false if sending could not be started. */
    suspend fun send(ctx: Context, address: String, body: String, subId: Int): Boolean = withContext(Dispatchers.IO) {
        if (body.isBlank()) return@withContext false
        val repo = ctx.app.repo
        val target = PhoneUtils.clean(address).ifEmpty { address.trim() }
        if (target.isEmpty()) return@withContext false
        val sub = resolveSub(ctx, subId)
        val m = repo.insertOutgoing(target, body, sub)
        if (!hasPermission(ctx)) {
            repo.setStatus(m.id, TYPE_FAILED)
            return@withContext false
        }
        try {
            val sm = manager(ctx, sub)
            val parts = sm.divideMessage(body)
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            for (i in parts.indices) {
                val intent = Intent(ctx, SmsStatusReceiver::class.java)
                    .setAction(ACTION_SENT)
                    .putExtra(EXTRA_ID, m.id)
                    .putExtra(EXTRA_LAST, i == parts.size - 1)
                sentIntents += PendingIntent.getBroadcast(
                    ctx, m.id.toInt() * 31 + i, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            sm.sendMultipartTextMessage(target, null, parts, sentIntents, null)
            true
        } catch (e: Exception) {
            repo.setStatus(m.id, TYPE_FAILED)
            false
        }
    }
}
