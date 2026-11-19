package dev.imhamid.messages.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dev.imhamid.messages.MainActivity
import dev.imhamid.messages.R
import dev.imhamid.messages.data.MessageEntity
import dev.imhamid.messages.receivers.NotificationActionReceiver

object Notifier {
    private const val CHANNEL = "messages"
    const val ACTION_REPLY = "dev.imhamid.messages.NOTIF_REPLY"
    const val ACTION_DELETE = "dev.imhamid.messages.NOTIF_DELETE"
    const val KEY_REPLY = "reply_text"
    const val EXTRA_KEY = "thread_key"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_MSG_ID = "message_id"
    const val EXTRA_SUB = "sub_id"

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(ctx.getString(R.string.channel_messages))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(ch)
    }

    fun show(ctx: Context, m: MessageEntity, name: String?) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val open = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("sms:" + Uri.encode(m.address)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, m.threadKey.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val base = Intent(ctx, NotificationActionReceiver::class.java)
            .putExtra(EXTRA_KEY, m.threadKey)
            .putExtra(EXTRA_ADDRESS, m.address)
            .putExtra(EXTRA_MSG_ID, m.id)
            .putExtra(EXTRA_SUB, m.subId)
        val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val replyPi = PendingIntent.getBroadcast(
            ctx, m.threadKey.hashCode() * 31 + 1, Intent(base).setAction(ACTION_REPLY),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable
        )
        val deletePi = PendingIntent.getBroadcast(
            ctx, m.threadKey.hashCode() * 31 + 2, Intent(base).setAction(ACTION_DELETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_reply, ctx.getString(R.string.notif_reply), replyPi
        )
            .addRemoteInput(RemoteInput.Builder(KEY_REPLY).setLabel(ctx.getString(R.string.notif_reply)).build())
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
        val deleteAction = NotificationCompat.Action.Builder(
            R.drawable.ic_delete, ctx.getString(R.string.notif_delete), deletePi
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_DELETE)
            .setShowsUserInterface(false)
            .build()
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name ?: m.address)
            .setContentText(m.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(m.body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(m.date)
            .setContentIntent(pi)
            .addAction(replyAction)
            .addAction(deleteAction)
            .build()
        try {
            nm.notify(m.threadKey.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context, key: String) = NotificationManagerCompat.from(ctx).cancel(key.hashCode())
}
