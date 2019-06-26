package im.vector.riotredesign.core.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import timber.log.Timber

class AlarmSyncBroadcastReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {

        //Aquire a lock to give enough time for the sync :/
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "riotx:fdroidSynclock").apply {
                acquire((10_000).toLong())
            }
        }


        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Timber.d("RestartBroadcastReceiver received intent")
        Intent(context, VectorSyncService::class.java).also {
            it.action = "SLOW"
            context.startService(it)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (ex: Throwable) {
                //TODO
                Timber.e(ex)
            }
        }

        scheduleAlarm(context, 30_000L)

        Timber.i("Alarm scheduled to restart service")
    }

    companion object {
        const val REQUEST_CODE = 0

        fun scheduleAlarm(context: Context, delay: Long) {
            //Reschedule
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java)
            val pIntent = PendingIntent.getBroadcast(context, AlarmSyncBroadcastReceiver.REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val firstMillis = System.currentTimeMillis() + delay
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, firstMillis, pIntent)
            }
        }

        fun cancelAlarm(context: Context) {
            val intent = Intent(context, AlarmSyncBroadcastReceiver::class.java)
            val pIntent = PendingIntent.getBroadcast(context, AlarmSyncBroadcastReceiver.REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmMgr.cancel(pIntent)
        }
    }
}
