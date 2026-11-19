package dev.imhamid.messages.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.imhamid.messages.MainActivity
import dev.imhamid.messages.R
import dev.imhamid.messages.data.MessageEntity

object Notifier {
    private const val CHANNEL = "messages"

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
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name ?: m.address)
            .setContentText(m.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(m.body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(m.date)
            .setContentIntent(pi)
            .build()
        try {
            nm.notify(m.threadKey.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context, key: String) = NotificationManagerCompat.from(ctx).cancel(key.hashCode())
}
