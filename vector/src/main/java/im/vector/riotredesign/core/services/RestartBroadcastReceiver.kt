package im.vector.riotredesign.core.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.core.content.ContextCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import im.vector.matrix.android.internal.session.sync.job.SyncService
import timber.log.Timber

class RestartBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Timber.d("RestartBroadcastReceiver received intent")
        Intent(context,VectorSyncService::class.java).also {
            it.action = "SLOW"
            context.startService(it)
            try {
                if (SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (ex: Throwable) {
                //TODO
                Timber.e(ex)
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 0
    }
}
