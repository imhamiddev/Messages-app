package dev.imhamid.messages.receivers

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.RemoteInput
import dev.imhamid.messages.app
import dev.imhamid.messages.data.TYPE_FAILED
import dev.imhamid.messages.data.TYPE_SENT
import dev.imhamid.messages.sms.Notifier
import dev.imhamid.messages.sms.Scheduler
import dev.imhamid.messages.sms.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Required for the default-SMS-app role: receives incoming text messages. */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val address = parts.firstOrNull()?.displayOriginatingAddress ?: return
        val body = parts.joinToString("") { it.displayMessageBody ?: "" }
        val subId = intent.getIntExtra("subscription", -1)
        runAsync {
            val app = context.app
            val stored = app.repo.onIncoming(address, body, subId) ?: return@runAsync
            if (app.repo.activeKey != stored.threadKey) {
                Notifier.show(context, stored, app.repo.contacts.nameFor(address))
            }
        }
    }
}

/** MMS is intentionally unsupported (text only), but the role requires a receiver. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/** Required for the default-SMS-app role ("respond via message" from the dialer). */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(SmsSender.EXTRA_ID, -1)
        if (id < 0) return
        val last = intent.getBooleanExtra(SmsSender.EXTRA_LAST, true)
        val ok = resultCode == Activity.RESULT_OK
        runAsync {
            val repo = context.app.repo
            if (!ok) repo.setStatus(id, TYPE_FAILED)
            else if (last) repo.setStatusIfSending(id, TYPE_SENT)
        }
    }
}

class ScheduledSendReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Scheduler.EXTRA_ID, -1)
        if (id < 0) return
        runAsync { Scheduler.run(context, id) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync { Scheduler.rearmAll(context) }
    }
}

/** Handles the Reply and Delete buttons of an incoming-message notification. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(Notifier.EXTRA_KEY) ?: return
        val address = intent.getStringExtra(Notifier.EXTRA_ADDRESS) ?: return
        when (intent.action) {
            Notifier.ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(Notifier.KEY_REPLY)?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    Notifier.cancel(context, key)
                    return
                }
                val sub = intent.getIntExtra(Notifier.EXTRA_SUB, -1)
                runAsync {
                    val repo = context.app.repo
                    SmsSender.send(context, address, text, repo.chatSim(key) ?: sub)
                    repo.markRead(key)
                    Notifier.cancel(context, key)
                }
            }
            Notifier.ACTION_DELETE -> {
                val id = intent.getLongExtra(Notifier.EXTRA_MSG_ID, -1)
                runAsync {
                    val repo = context.app.repo
                    if (id >= 0) repo.trashMessages(listOf(id))
                    repo.markRead(key)
                    Notifier.cancel(context, key)
                }
            }
        }
    }
}
