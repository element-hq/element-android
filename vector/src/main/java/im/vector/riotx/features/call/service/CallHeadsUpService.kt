/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.call.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import im.vector.riotx.R

class CallHeadsUpService : Service() {

    private val CHANNEL_ID = "CallChannel"
    private val CHANNEL_NAME = "Call Channel"
    private val CHANNEL_DESCRIPTION = "Call Notifications"

    private val binder: IBinder = CallHeadsUpServiceBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //val callHeadsUpServiceArgs: CallHeadsUpServiceArgs? = intent?.extras?.getParcelable(EXTRA_CALL_HEADS_UP_SERVICE_PARAMS)

        val answerCallActionReceiver = Intent(applicationContext, CallHeadsUpActionReceiver::class.java).apply {
            putExtra(EXTRA_CALL_ACTION_KEY, CALL_ACTION_ANSWER)
        }
        val rejectCallActionReceiver = Intent(applicationContext, CallHeadsUpActionReceiver::class.java).apply {
            putExtra(EXTRA_CALL_ACTION_KEY, CALL_ACTION_REJECT)
        }

        val answerCallPendingIntent = PendingIntent.getBroadcast(applicationContext, CALL_ACTION_ANSWER, answerCallActionReceiver, PendingIntent.FLAG_UPDATE_CURRENT)
        val rejectCallPendingIntent = PendingIntent.getBroadcast(applicationContext, CALL_ACTION_REJECT, rejectCallActionReceiver, PendingIntent.FLAG_UPDATE_CURRENT)

        createNotificationChannel()

        val notification = NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Title")
                .setContentText("Content")
                .setSmallIcon(R.drawable.ic_call_incoming)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .addAction(R.drawable.ic_call_incoming, getString(R.string.call_notification_answer), answerCallPendingIntent)
                .addAction(R.drawable.ic_call_incoming, getString(R.string.call_notification_reject), rejectCallPendingIntent)
                .setAutoCancel(true)
                .setSound(Uri.parse("android.resource://" + applicationContext.packageName + "/ring.ogg"))
                .setFullScreenIntent(answerCallPendingIntent, true)
                .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESCRIPTION
            setSound(
                    Uri.parse("android.resource://" + applicationContext.packageName + "/ring.ogg"),
                    AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .build()
                    )
        }
        applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    inner class CallHeadsUpServiceBinder : Binder() {

        fun getService() = this@CallHeadsUpService
    }

    companion object {
        private const val EXTRA_CALL_HEADS_UP_SERVICE_PARAMS = "EXTRA_CALL_PARAMS"

        const val EXTRA_CALL_ACTION_KEY = "EXTRA_CALL_ACTION_KEY"
        const val CALL_ACTION_ANSWER = 100
        const val CALL_ACTION_REJECT = 101

        private const val NOTIFICATION_ID = 999

        fun newInstance(context: Context, callerDisplayName: String, isIncomingCall: Boolean, isVideoCall: Boolean): Intent {
            val args = CallHeadsUpServiceArgs(callerDisplayName, isIncomingCall, isVideoCall)
            return Intent(context, CallHeadsUpService::class.java).apply {
                putExtra(EXTRA_CALL_HEADS_UP_SERVICE_PARAMS, args)
            }
        }
    }
}
