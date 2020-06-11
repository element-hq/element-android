// /*
// * Copyright (c) 2020 New Vector Ltd
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package im.vector.riotx.features.call.service
//
// import android.app.Notification
// import android.app.NotificationChannel
// import android.app.NotificationManager
// import android.app.PendingIntent
// import android.app.Service
// import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
// import android.content.Context
// import android.content.Intent
// import android.media.AudioAttributes
// import android.net.Uri
// import android.os.Binder
// import android.os.Build
// import android.os.IBinder
// import androidx.core.app.NotificationCompat
// import androidx.core.content.ContextCompat
// import androidx.core.graphics.drawable.IconCompat
// import im.vector.matrix.android.api.session.call.MxCallDetail
// import im.vector.riotx.R
// import im.vector.riotx.core.extensions.vectorComponent
// import im.vector.riotx.features.call.VectorCallActivity
// import im.vector.riotx.features.notifications.NotificationUtils
// import im.vector.riotx.features.themes.ThemeUtils
//
// class CallHeadsUpService : Service() {
// //
// //    private val CHANNEL_ID = "CallChannel"
// //    private val CHANNEL_NAME = "Call Channel"
// //    private val CHANNEL_DESCRIPTION = "Call Notifications"
//
//    lateinit var notificationUtils: NotificationUtils
//    private val binder: IBinder = CallHeadsUpServiceBinder()
//
//    override fun onBind(intent: Intent): IBinder? {
//        return binder
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//       notificationUtils = vectorComponent().notificationUtils()
//    }
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val callHeadsUpServiceArgs: CallHeadsUpServiceArgs? = intent?.extras?.getParcelable(EXTRA_CALL_HEADS_UP_SERVICE_PARAMS)
//
// //        createNotificationChannel()
//
// //        val title = callHeadsUpServiceArgs?.otherUserId ?: ""
// //        val description = when {
// //            callHeadsUpServiceArgs?.isIncomingCall == false -> getString(R.string.call_ring)
// //            callHeadsUpServiceArgs?.isVideoCall == true     -> getString(R.string.incoming_video_call)
// //            else                                            -> getString(R.string.incoming_voice_call)
// //        }
//
//        // val actions = if (callHeadsUpServiceArgs?.isIncomingCall == true) createAnswerAndRejectActions() else emptyList()
//
//        notificationUtils.buildIncomingCallNotification(
//                callHeadsUpServiceArgs?.isVideoCall ?: false,
//                callHeadsUpServiceArgs?.otherUserId ?: "",
//                callHeadsUpServiceArgs?.roomId ?: "",
//                callHeadsUpServiceArgs?.callId ?: ""
//        ).let {
//            startForeground(NOTIFICATION_ID, it)
//        }
// //        createNotification(title, description, actions).also {
// //            startForeground(NOTIFICATION_ID, it)
// //        }
//
//        return START_STICKY
//    }
//
// //    private fun createNotification(title: String, content: String, actions: List<NotificationCompat.Action>): Notification {
// //        val answerCallActionReceiver = Intent(applicationContext, CallHeadsUpActionReceiver::class.java).apply {
// //            putExtra(EXTRA_CALL_ACTION_KEY, CALL_ACTION_ANSWER)
// //        }.let {
// //            PendingIntent.getBroadcast(applicationContext, CALL_ACTION_ANSWER, it, PendingIntent.FLAG_UPDATE_CURRENT)
// //        }
// //        return NotificationCompat
// //                .Builder(applicationContext, CHANNEL_ID)
// //                .setContentTitle(title)
// //                .setContentText(content)
// //                .setSmallIcon(R.drawable.ic_call)
// //                .setPriority(NotificationCompat.PRIORITY_MAX)
// //                .setWhen(0)
// //                .setCategory(NotificationCompat.CATEGORY_CALL)
// //                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
// //                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
// //                .setSound(Uri.parse(SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/raw/ring.ogg"))
// //                .setVibrate(longArrayOf(1000, 1000))
// //                .setFullScreenIntent(answerCallActionReceiver, true)
// //                .setOngoing(true)
// //                //.setStyle(NotificationCompat.BigTextStyle())
// //                .setAutoCancel(true)
// //                .apply { actions.forEach { addAction(it) } }
// //                .build()
// //    }
//
// //    private fun createAnswerAndRejectActions(): List<NotificationCompat.Action> {
// //        val answerCallActionReceiver = Intent(applicationContext, CallHeadsUpActionReceiver::class.java).apply {
// //            putExtra(EXTRA_CALL_ACTION_KEY, CALL_ACTION_ANSWER)
// //        }
// //        val rejectCallActionReceiver = Intent(applicationContext, CallHeadsUpActionReceiver::class.java).apply {
// //            putExtra(EXTRA_CALL_ACTION_KEY, CALL_ACTION_REJECT)
// //        }
// //        val answerCallPendingIntent = PendingIntent.getBroadcast(applicationContext, CALL_ACTION_ANSWER, answerCallActionReceiver, PendingIntent.FLAG_UPDATE_CURRENT)
// //        val rejectCallPendingIntent = PendingIntent.getBroadcast(applicationContext, CALL_ACTION_REJECT, rejectCallActionReceiver, PendingIntent.FLAG_UPDATE_CURRENT)
// //
// //        return listOf(
// //                NotificationCompat.Action(
// //                        R.drawable.ic_call,
// //                        //IconCompat.createWithResource(applicationContext, R.drawable.ic_call).setTint(ContextCompat.getColor(applicationContext, R.color.riotx_positive_accent)),
// //                        getString(R.string.call_notification_answer),
// //                        answerCallPendingIntent
// //                ),
// //                NotificationCompat.Action(
// //                        IconCompat.createWithResource(applicationContext, R.drawable.ic_call_end).setTint(ContextCompat.getColor(applicationContext, R.color.riotx_notice)),
// //                        getString(R.string.call_notification_reject),
// //                        rejectCallPendingIntent)
// //        )
// //    }
//
// //    private fun createNotificationChannel() {
// //        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
// //
// //        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
// //            description = CHANNEL_DESCRIPTION
// //            setSound(
// //                    Uri.parse(SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/raw/ring.ogg"),
// //                    AudioAttributes
// //                            .Builder()
// //                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
// //                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
// //                            .build()
// //            )
// //            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
// //            enableVibration(true)
// //            enableLights(true)
// //        }
// //        applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
// //    }
//
//    inner class CallHeadsUpServiceBinder : Binder() {
//
//        fun getService() = this@CallHeadsUpService
//    }
//
//
//    companion object {
//        private const val EXTRA_CALL_HEADS_UP_SERVICE_PARAMS = "EXTRA_CALL_PARAMS"
//
//        const val EXTRA_CALL_ACTION_KEY = "EXTRA_CALL_ACTION_KEY"
// //        const val CALL_ACTION_ANSWER = 100
//        const val CALL_ACTION_REJECT = 101
//
//        private const val NOTIFICATION_ID = 999
//
//        fun newInstance(context: Context, mxCall: MxCallDetail): Intent {
//            val args = CallHeadsUpServiceArgs(mxCall.callId, mxCall.roomId, mxCall.otherUserId, !mxCall.isOutgoing, mxCall.isVideoCall)
//            return Intent(context, CallHeadsUpService::class.java).apply {
//                putExtra(EXTRA_CALL_HEADS_UP_SERVICE_PARAMS, args)
//            }
//        }
//    }
// }
