package com.chetna.frostexpress

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Local reminders while the game is closed: "your train's idle earnings are full" and "your daily gift is ready".
 * Scheduled with inexact alarms when the app goes to the background, cancelled when it comes back.
 * Tapping a reminder opens the game.
 */
object Reminders {
    private const val CHANNEL = "train_reminders"
    const val KIND_IDLE = 1
    const val KIND_GIFT = 2

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Train reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily gift and full idle earnings"
            }
        )
    }

    private fun pending(ctx: Context, kind: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, kind, Intent(ctx, ReminderReceiver::class.java).putExtra("kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Called when the game goes to the background. */
    fun schedule(ctx: Context, idleFullInSec: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        am.set(AlarmManager.RTC, now + idleFullInSec * 1000L, pending(ctx, KIND_IDLE))
        // the daily gift: tomorrow at 10:00 local time
        val tomorrow10 = java.time.LocalDate.now().plusDays(1).atTime(10, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.set(AlarmManager.RTC, tomorrow10, pending(ctx, KIND_GIFT))
    }

    /** Called when the game comes back: nothing to remind about while playing. */
    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(ctx, KIND_IDLE))
        am.cancel(pending(ctx, KIND_GIFT))
        NotificationManagerCompat.from(ctx).cancelAll()
    }

    fun show(ctx: Context, kind: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 10 + kind,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = when (kind) {
            KIND_GIFT -> "Your daily gift is ready" to "Coins and gems are waiting at the station. Keep your streak going!"
            else -> "The coal bins are full" to "Your train earned all it can while you were away. Hop back on to collect!"
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFFE8897F.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(kind, n) } catch (e: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("frost_express", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("remind", true)) return
        val kind = intent.getIntExtra("kind", Reminders.KIND_IDLE)
        // no gift reminder if today's gift was already taken
        if (kind == Reminders.KIND_GIFT && prefs.getLong("dailyDay", -1L) == java.time.LocalDate.now().toEpochDay()) return
        Reminders.show(context, kind)
    }
}
