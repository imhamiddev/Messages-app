package dev.imhamid.messages.sms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.imhamid.messages.app
import dev.imhamid.messages.data.SCHED_FAILED
import dev.imhamid.messages.data.SCHED_PENDING
import dev.imhamid.messages.data.SCHED_SENT
import dev.imhamid.messages.data.ScheduledEntity
import dev.imhamid.messages.receivers.ScheduledSendReceiver

/** Time-based sending: one exact alarm per pending scheduled message. */
object Scheduler {
    const val ACTION = "dev.imhamid.messages.SCHEDULED_SEND"
    const val EXTRA_ID = "id"

    private fun pi(ctx: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id.toInt(),
            Intent(ctx, ScheduledSendReceiver::class.java).setAction(ACTION).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun alarms(ctx: Context): AlarmManager = ctx.getSystemService(AlarmManager::class.java)

    fun canExact(ctx: Context): Boolean = Build.VERSION.SDK_INT < 31 || alarms(ctx).canScheduleExactAlarms()

    fun arm(ctx: Context, id: Long, at: Long) {
        val t = maxOf(at, System.currentTimeMillis() + 1500)
        val am = alarms(ctx)
        val p = pi(ctx, id)
        try {
            if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
        }
    }

    fun disarm(ctx: Context, id: Long) = alarms(ctx).cancel(pi(ctx, id))

    suspend fun create(ctx: Context, address: String, body: String, sendAt: Long, subId: Int): ScheduledEntity {
        val s = ctx.app.repo.addScheduled(address, body, sendAt, subId)
        arm(ctx, s.id, s.sendAt)
        return s
    }

    suspend fun remove(ctx: Context, id: Long) {
        disarm(ctx, id)
        ctx.app.repo.scheduled.delete(id)
    }

    suspend fun rearmAll(ctx: Context) {
        ctx.app.repo.scheduled.pending().forEach { arm(ctx, it.id, it.sendAt) }
    }

    suspend fun run(ctx: Context, id: Long) {
        val dao = ctx.app.repo.scheduled
        val s = dao.get(id) ?: return
        if (s.state != SCHED_PENDING) return
        val ok = SmsSender.send(ctx, s.address, s.body, s.subId)
        dao.setState(id, if (ok) SCHED_SENT else SCHED_FAILED)
    }
}
