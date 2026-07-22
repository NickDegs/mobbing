package com.nickdegs.mobbing

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.random.Random

/**
 * Yerel "ofis olayı" bildirimleri — günde 1 rastgele saat (10:00-20:00).
 * Tamamen cihazda üretilir; sunucu yok, veri yok.
 */
object Notif {
    private const val CH = "office_events"

    fun scheduleDaily(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH, "Office Events", NotificationManager.IMPORTANCE_DEFAULT))

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, NotifReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, if (get(Calendar.HOUR_OF_DAY) >= 20) 1 else 0)
            set(Calendar.HOUR_OF_DAY, Random.nextInt(10, 20))
            set(Calendar.MINUTE, Random.nextInt(0, 60))
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }

    fun show(ctx: Context) {
        val bodies = ctx.resources.getStringArray(R.array.notif_bodies)
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(bodies[Random.nextInt(bodies.size)])
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(ctx, 1,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try { nm.notify(Random.nextInt(1000), n) } catch (_: SecurityException) {}
    }
}

class NotifReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Notif.show(context)
}
