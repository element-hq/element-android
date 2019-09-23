/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.riotx.features.notifications

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.startNotificationChannelSettingsIntent
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.home.room.detail.RoomDetailActivity
import im.vector.riotx.features.home.room.detail.RoomDetailArgs
import im.vector.riotx.features.settings.VectorPreferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Util class for creating notifications.
 * Note: Cannot inject ColorProvider in the constructor, because it requires an Activity
 */
@Singleton
class NotificationUtils @Inject constructor(private val context: Context,
                                            private val stringProvider: StringProvider,
                                            private val vectorPreferences: VectorPreferences) {

    companion object {
        /* ==========================================================================================
         * IDs for notifications
         * ========================================================================================== */

        /**
         * Identifier of the foreground notification used to keep the application alive
         * when it runs in background.
         * This notification, which is not removable by the end user, displays what
         * the application is doing while in background.
         */
        const val NOTIFICATION_ID_FOREGROUND_SERVICE = 61

        /* ==========================================================================================
         * IDs for actions
         * ========================================================================================== */

        const val JOIN_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.JOIN_ACTION"
        const val REJECT_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.REJECT_ACTION"
        private const val QUICK_LAUNCH_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.QUICK_LAUNCH_ACTION"
        const val MARK_ROOM_READ_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.MARK_ROOM_READ_ACTION"
        const val SMART_REPLY_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.SMART_REPLY_ACTION"
        const val DISMISS_SUMMARY_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.DISMISS_SUMMARY_ACTION"
        const val DISMISS_ROOM_NOTIF_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.DISMISS_ROOM_NOTIF_ACTION"
        private const val TAP_TO_VIEW_ACTION = "${BuildConfig.APPLICATION_ID}.NotificationActions.TAP_TO_VIEW_ACTION"

        /* ==========================================================================================
         * IDs for channels
         * ========================================================================================== */

        // on devices >= android O, we need to define a channel for each notifications
        private const val LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID = "LISTEN_FOR_EVENTS_NOTIFICATION_CHANNEL_ID"

        private const val NOISY_NOTIFICATION_CHANNEL_ID = "DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID"

        private const val SILENT_NOTIFICATION_CHANNEL_ID = "DEFAULT_SILENT_NOTIFICATION_CHANNEL_ID_V2"
        private const val CALL_NOTIFICATION_CHANNEL_ID = "CALL_NOTIFICATION_CHANNEL_ID_V2"

        fun supportNotificationChannels() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        fun openSystemSettingsForSilentCategory(fragment: Fragment) {
            startNotificationChannelSettingsIntent(fragment, SILENT_NOTIFICATION_CHANNEL_ID)
        }

        fun openSystemSettingsForNoisyCategory(fragment: Fragment) {
            startNotificationChannelSettingsIntent(fragment, NOISY_NOTIFICATION_CHANNEL_ID)
        }

        fun openSystemSettingsForCallCategory(fragment: Fragment) {
            startNotificationChannelSettingsIntent(fragment, CALL_NOTIFICATION_CHANNEL_ID)
        }
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    /* ==========================================================================================
     * Channel names
     * ========================================================================================== */

    /**
     * Create notification channels.
     */
    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannels() {
        if (!supportNotificationChannels()) {
            return
        }

        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)

        //Migration - the noisy channel was deleted and recreated when sound preference was changed (id was DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID_BASE
        // + currentTimeMillis).
        //Now the sound can only be change directly in system settings, so for app upgrading we are deleting this former channel
        //Starting from this version the channel will not be dynamic
        for (channel in notificationManager.notificationChannels) {
            val channelId = channel.id
            val legacyBaseName = "DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID_BASE"
            if (channelId.startsWith(legacyBaseName)) {
                notificationManager.deleteNotificationChannel(channelId)
            }
        }
        //Migration - Remove deprecated channels
        for (channelId in listOf("DEFAULT_SILENT_NOTIFICATION_CHANNEL_ID", "CALL_NOTIFICATION_CHANNEL_ID")) {
            notificationManager.getNotificationChannel(channelId)?.let {
                notificationManager.deleteNotificationChannel(channelId)
            }
        }

        /**
         * Default notification importance: shows everywhere, makes noise, but does not visually
         * intrude.
         */
        notificationManager.createNotificationChannel(NotificationChannel(NOISY_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(R.string.notification_noisy_notifications),
                NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    description = stringProvider.getString(R.string.notification_noisy_notifications)
                    enableVibration(true)
                    enableLights(true)
                    lightColor = accentColor
                })

        /**
         * Low notification importance: shows everywhere, but is not intrusive.
         */
        notificationManager.createNotificationChannel(NotificationChannel(SILENT_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(R.string.notification_silent_notifications),
                NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = stringProvider.getString(R.string.notification_silent_notifications)
                    setSound(null, null)
                    enableLights(true)
                    lightColor = accentColor
                })

        notificationManager.createNotificationChannel(NotificationChannel(LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(R.string.notification_listening_for_events),
                NotificationManager.IMPORTANCE_MIN)
                .apply {
                    description = stringProvider.getString(R.string.notification_listening_for_events)
                    setSound(null, null)
                    setShowBadge(false)
                })

        notificationManager.createNotificationChannel(NotificationChannel(CALL_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(R.string.call),
                NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = stringProvider.getString(R.string.call)
                    setSound(null, null)
                    enableLights(true)
                    lightColor = accentColor
                })
    }

    /**
     * Build a polling thread listener notification
     *
     * @param subTitleResId subtitle string resource Id of the notification
     * @return the polling thread listener notification
     */
    @SuppressLint("NewApi")
    fun buildForegroundServiceNotification(@StringRes subTitleResId: Int, withProgress: Boolean = true): Notification {
        // build the pending intent go to the home screen if this is clicked.
        val i = Intent(context, HomeActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pi = PendingIntent.getActivity(context, 0, i, 0)

        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)

        val builder = NotificationCompat.Builder(context, LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(subTitleResId))
                .setSmallIcon(R.drawable.sync)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setColor(accentColor)
                .setContentIntent(pi)
                .apply {
                    if (withProgress) {
                        setProgress(0, 0, true)
                    }
                }

        // PRIORITY_MIN should not be used with Service#startForeground(int, Notification)
        builder.priority = NotificationCompat.PRIORITY_LOW
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            builder.priority = NotificationCompat.PRIORITY_MIN
//        }

        val notification = builder.build()

        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // some devices crash if this field is not set
            // even if it is deprecated

            // setLatestEventInfo() is deprecated on Android M, so we try to use
            // reflection at runtime, to avoid compiler error: "Cannot resolve method.."
            try {
                val deprecatedMethod = notification.javaClass
                        .getMethod("setLatestEventInfo",
                                Context::class.java,
                                CharSequence::class.java,
                                CharSequence::class.java,
                                PendingIntent::class.java)
                deprecatedMethod.invoke(notification, context, stringProvider.getString(R.string.app_name), stringProvider.getString(subTitleResId), pi)
            } catch (ex: Exception) {
                Timber.e(ex, "## buildNotification(): Exception - setLatestEventInfo() Msg=")
            }

        }
        return notification
    }

    /**
     * Build an incoming call notification.
     * This notification starts the VectorHomeActivity which is in charge of centralizing the incoming call flow.
     *
     * @param isVideo  true if this is a video call, false for voice call
     * @param roomName the room name in which the call is pending.
     * @param matrixId the matrix id
     * @param callId   the call id.
     * @return the call notification.
     */
    @SuppressLint("NewApi")
    fun buildIncomingCallNotification(isVideo: Boolean,
                                      roomName: String,
                                      matrixId: String,
                                      callId: String): Notification {
        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)

        val builder = NotificationCompat.Builder(context, CALL_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(ensureTitleNotEmpty(roomName))
                .apply {
                    if (isVideo) {
                        setContentText(stringProvider.getString(R.string.incoming_video_call))
                    } else {
                        setContentText(stringProvider.getString(R.string.incoming_voice_call))
                    }
                }
                .setSmallIcon(R.drawable.incoming_call_notification_transparent)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setLights(accentColor, 500, 500)

        //Compat: Display the incoming call notification on the lock screen
        builder.priority = NotificationCompat.PRIORITY_MAX

        // clear the activity stack to home activity
        val intent = Intent(context, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        // TODO .putExtra(VectorHomeActivity.EXTRA_CALL_SESSION_ID, matrixId)
        // TODO .putExtra(VectorHomeActivity.EXTRA_CALL_ID, callId)

        // Recreate the back stack
        val stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(HomeActivity::class.java)
                .addNextIntent(intent)


        // android 4.3 issue
        // use a generator for the private requestCode.
        // When using 0, the intent is not created/launched when the user taps on the notification.
        //
        val pendingIntent = stackBuilder.getPendingIntent(Random().nextInt(1000), PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    /**
     * Build a pending call notification
     *
     * @param isVideo  true if this is a video call, false for voice call
     * @param roomName the room name in which the call is pending.
     * @param roomId   the room Id
     * @param matrixId the matrix id
     * @param callId   the call id.
     * @return the call notification.
     */
    @SuppressLint("NewApi")
    fun buildPendingCallNotification(isVideo: Boolean,
                                     roomName: String,
                                     roomId: String,
                                     matrixId: String,
                                     callId: String): Notification {

        val builder = NotificationCompat.Builder(context, CALL_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(ensureTitleNotEmpty(roomName))
                .apply {
                    if (isVideo) {
                        setContentText(stringProvider.getString(R.string.video_call_in_progress))
                    } else {
                        setContentText(stringProvider.getString(R.string.call_in_progress))
                    }
                }
                .setSmallIcon(R.drawable.incoming_call_notification_transparent)
                .setCategory(NotificationCompat.CATEGORY_CALL)

        // Display the pending call notification on the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.priority = NotificationCompat.PRIORITY_MAX
        }

        /* TODO
        // Build the pending intent for when the notification is clicked
        val roomIntent = Intent(context, VectorRoomActivity::class.java)
                .putExtra(VectorRoomActivity.EXTRA_ROOM_ID, roomId)
                .putExtra(VectorRoomActivity.EXTRA_MATRIX_ID, matrixId)
                .putExtra(VectorRoomActivity.EXTRA_START_CALL_ID, callId)

        // Recreate the back stack
        val stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(VectorRoomActivity::class.java)
                .addNextIntent(roomIntent)

        // android 4.3 issue
        // use a generator for the private requestCode.
        // When using 0, the intent is not created/launched when the user taps on the notification.
        //
        val pendingIntent = stackBuilder.getPendingIntent(Random().nextInt(1000), PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingIntent)
        */

        return builder.build()
    }

    /**
     * Build a temporary (because service will be stopped just after) notification for the CallService, when a call is ended
     */
    fun buildCallEndedNotification(): Notification {
        return NotificationCompat.Builder(context, CALL_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(R.string.call_ended))
                .setSmallIcon(R.drawable.ic_material_call_end_grey)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build()
    }

    /**
     * Build a notification for a Room
     */
    fun buildMessagesListNotification(messageStyle: NotificationCompat.MessagingStyle,
                                      roomInfo: RoomEventGroupInfo,
                                      largeIcon: Bitmap?,
                                      lastMessageTimestamp: Long,
                                      senderDisplayNameForReplyCompat: String?): Notification {
        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val openRoomIntent = buildOpenRoomIntent(roomInfo.roomId)
        val smallIcon = R.drawable.ic_status_bar

        val channelID = if (roomInfo.shouldBing) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID
        return NotificationCompat.Builder(context, channelID)
                .setWhen(lastMessageTimestamp)
                // MESSAGING_STYLE sets title and content for API 16 and above devices.
                .setStyle(messageStyle)

                // A category allows groups of notifications to be ranked and filtered – per user or system settings.
                // For example, alarm notifications should display before promo notifications, or message from known contact
                // that can be displayed in not disturb mode if white listed (the later will need compat28.x)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

                // Title for API < 16 devices.
                .setContentTitle(roomInfo.roomDisplayName)
                // Content for API < 16 devices.
                .setContentText(stringProvider.getString(R.string.notification_new_messages))

                // Number of new notifications for API <24 (M and below) devices.
                .setSubText(stringProvider.getQuantityString(R.plurals.room_new_messages_notification, messageStyle.messages.size, messageStyle.messages.size))

                // Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. But we want a custom grouping, so we specify the groupID
                // TODO Group should be current user display name
                .setGroup(stringProvider.getString(R.string.app_name))

                //In order to avoid notification making sound twice (due to the summary notification)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

                .setSmallIcon(smallIcon)

                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(accentColor)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .apply {
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    if (roomInfo.shouldBing) {
                        //Compat
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        priority = NotificationCompat.PRIORITY_LOW
                    }

                    //Add actions and notification intents
                    // Mark room as read
                    val markRoomReadIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    markRoomReadIntent.action = MARK_ROOM_READ_ACTION
                    markRoomReadIntent.data = Uri.parse("foobar://${roomInfo.roomId}")
                    markRoomReadIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomInfo.roomId)
                    val markRoomReadPendingIntent = PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), markRoomReadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)

                    addAction(NotificationCompat.Action(
                            R.drawable.ic_material_done_all_white,
                            stringProvider.getString(R.string.action_mark_room_read),
                            markRoomReadPendingIntent))

                    // Quick reply
                    if (!roomInfo.hasSmartReplyError) {
                        buildQuickReplyIntent(roomInfo.roomId, senderDisplayNameForReplyCompat)?.let { replyPendingIntent ->
                            val remoteInput = RemoteInput.Builder(NotificationBroadcastReceiver.KEY_TEXT_REPLY)
                                    .setLabel(stringProvider.getString(R.string.action_quick_reply))
                                    .build()
                            NotificationCompat.Action.Builder(R.drawable.vector_notification_quick_reply,
                                    stringProvider.getString(R.string.action_quick_reply), replyPendingIntent)
                                    .addRemoteInput(remoteInput)
                                    .build()?.let {
                                        addAction(it)
                                    }
                        }
                    }

                    if (openRoomIntent != null) {
                        setContentIntent(openRoomIntent)
                    }

                    if (largeIcon != null) {
                        setLargeIcon(largeIcon)
                    }

                    val intent = Intent(context, NotificationBroadcastReceiver::class.java)
                    intent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomInfo.roomId)
                    intent.action = DISMISS_ROOM_NOTIF_ACTION
                    val pendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                            System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    setDeleteIntent(pendingIntent)
                }
                .build()
    }


    fun buildRoomInvitationNotification(inviteNotifiableEvent: InviteNotifiableEvent,
                                        matrixId: String): Notification {
        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val smallIcon = R.drawable.ic_status_bar

        val channelID = if (inviteNotifiableEvent.noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID

        return NotificationCompat.Builder(context, channelID)
                .setContentTitle(stringProvider.getString(R.string.app_name))
                .setContentText(inviteNotifiableEvent.description)
                .setGroup(stringProvider.getString(R.string.app_name))
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setSmallIcon(smallIcon)
                .setColor(accentColor)
                .apply {
                    val roomId = inviteNotifiableEvent.roomId
                    // offer to type a quick reject button
                    val rejectIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    rejectIntent.action = REJECT_ACTION
                    rejectIntent.data = Uri.parse("foobar://$roomId&$matrixId")
                    rejectIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
                    val rejectIntentPendingIntent = PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), rejectIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)

                    addAction(
                            R.drawable.vector_notification_reject_invitation,
                            stringProvider.getString(R.string.reject),
                            rejectIntentPendingIntent)

                    // offer to type a quick accept button
                    val joinIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    joinIntent.action = JOIN_ACTION
                    joinIntent.data = Uri.parse("foobar://$roomId&$matrixId")
                    rejectIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
                    val joinIntentPendingIntent = PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), joinIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(
                            R.drawable.vector_notification_accept_invitation,
                            stringProvider.getString(R.string.join),
                            joinIntentPendingIntent)

                    val contentIntent = Intent(context, HomeActivity::class.java)
                    contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    //pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
                    contentIntent.data = Uri.parse("foobar://" + inviteNotifiableEvent.eventId)
                    setContentIntent(PendingIntent.getActivity(context, 0, contentIntent, 0))

                    if (inviteNotifiableEvent.noisy) {
                        //Compat
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                    setAutoCancel(true)
                }
                .build()
    }

    fun buildSimpleEventNotification(simpleNotifiableEvent: SimpleNotifiableEvent,
                                     matrixId: String): Notification {
        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val smallIcon = R.drawable.ic_status_bar

        val channelID = if (simpleNotifiableEvent.noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID

        return NotificationCompat.Builder(context, channelID)
                .setContentTitle(stringProvider.getString(R.string.app_name))
                .setContentText(simpleNotifiableEvent.description)
                .setGroup(stringProvider.getString(R.string.app_name))
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setSmallIcon(smallIcon)
                .setColor(accentColor)
                .setAutoCancel(true)
                .apply {
                    val contentIntent = Intent(context, HomeActivity::class.java)
                    contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    //pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
                    contentIntent.data = Uri.parse("foobar://" + simpleNotifiableEvent.eventId)
                    setContentIntent(PendingIntent.getActivity(context, 0, contentIntent, 0))

                    if (simpleNotifiableEvent.noisy) {
                        //Compat
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                    setAutoCancel(true)
                }
                .build()
    }

    private fun buildOpenRoomIntent(roomId: String): PendingIntent? {
        val roomIntentTap = RoomDetailActivity.newIntent(context, RoomDetailArgs(roomId))
        roomIntentTap.action = TAP_TO_VIEW_ACTION
        //pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
        roomIntentTap.data = Uri.parse("foobar://openRoom?$roomId")

        // Recreate the back stack
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(Intent(context, HomeActivity::class.java))
                .addNextIntent(roomIntentTap)
                .getPendingIntent(System.currentTimeMillis().toInt(), PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildOpenHomePendingIntentForSummary(): PendingIntent {
        val intent = HomeActivity.newIntent(context, clearNotification = true)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.data = Uri.parse("foobar://tapSummary")
        return PendingIntent.getActivity(context, Random().nextInt(1000), intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /*
        Direct reply is new in Android N, and Android already handles the UI, so the right pending intent
        here will ideally be a Service/IntentService (for a long running background task) or a BroadcastReceiver,
         which runs on the UI thread. It also works without unlocking, making the process really fluid for the user.
        However, for Android devices running Marshmallow and below (API level 23 and below),
        it will be more appropriate to use an activity. Since you have to provide your own UI.
     */
    private fun buildQuickReplyIntent(roomId: String, senderName: String?): PendingIntent? {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(context, NotificationBroadcastReceiver::class.java)
            intent.action = SMART_REPLY_ACTION
            intent.data = Uri.parse("foobar://$roomId")
            intent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
            return PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            /*
            TODO
            if (!LockScreenActivity.isDisplayingALockScreenActivity()) {
                // start your activity for Android M and below
                val quickReplyIntent = Intent(context, LockScreenActivity::class.java)
                quickReplyIntent.putExtra(LockScreenActivity.EXTRA_ROOM_ID, roomId)
                quickReplyIntent.putExtra(LockScreenActivity.EXTRA_SENDER_NAME, senderName ?: "")

                // the action must be unique else the parameters are ignored
                quickReplyIntent.action = QUICK_LAUNCH_ACTION
                quickReplyIntent.data = Uri.parse("foobar://$roomId")
                return PendingIntent.getActivity(context, 0, quickReplyIntent, 0)
            }
             */
        }
        return null
    }

    //// Number of new notifications for API <24 (M and below) devices.
    /**
     * Build the summary notification
     */
    fun buildSummaryListNotification(style: NotificationCompat.InboxStyle,
                                     compatSummary: String,
                                     noisy: Boolean,
                                     lastMessageTimestamp: Long): Notification {
        val accentColor = ContextCompat.getColor(context, R.color.notification_accent_color)
        val smallIcon = R.drawable.ic_status_bar

        return NotificationCompat.Builder(context, if (noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID)
                // used in compat < N, after summary is built based on child notifications
                .setWhen(lastMessageTimestamp)
                .setStyle(style)
                .setContentTitle(stringProvider.getString(R.string.app_name))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSmallIcon(smallIcon)
                //set content text to support devices running API level < 24
                .setContentText(compatSummary)
                .setGroup(stringProvider.getString(R.string.app_name))
                //set this notification as the summary for the group
                .setGroupSummary(true)
                .setColor(accentColor)
                .apply {
                    if (noisy) {
                        //Compat
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        //compat
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                }
                .setContentIntent(buildOpenHomePendingIntentForSummary())
                .setDeleteIntent(getDismissSummaryPendingIntent())
                .build()
    }

    private fun getDismissSummaryPendingIntent(): PendingIntent {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        intent.action = DISMISS_SUMMARY_ACTION
        intent.data = Uri.parse("foobar://deleteSummary")
        return PendingIntent.getBroadcast(context.applicationContext,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun showNotificationMessage(tag: String?, id: Int, notification: Notification) {
        notificationManager.notify(tag, id, notification)
    }

    fun cancelNotificationMessage(tag: String?, id: Int) {
        notificationManager.cancel(tag, id)
    }

    /**
     * Cancel the foreground notification service
     */
    fun cancelNotificationForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID_FOREGROUND_SERVICE)
    }

    /**
     * Cancel all the notification
     */
    fun cancelAllNotifications() {
        // Keep this try catch (reported by GA)
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Timber.e(e, "## cancelAllNotifications() failed " + e.message)
        }
    }

    /**
     * Return true it the user has enabled the do not disturb mode
     */
    fun isDoNotDisturbModeOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        // We cannot use NotificationManagerCompat here.
        val setting = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).currentInterruptionFilter

        return setting == NotificationManager.INTERRUPTION_FILTER_NONE
                || setting == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    private fun ensureTitleNotEmpty(title: String?): CharSequence {
        if (title.isNullOrBlank()) {
            return stringProvider.getString(R.string.app_name)
        }

        return title
    }
}
