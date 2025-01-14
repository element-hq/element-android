/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("UNUSED_PARAMETER")

package im.vector.app.features.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.AttrRes
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.Fragment
import im.vector.app.R
import im.vector.app.core.extensions.createIgnoredUri
import im.vector.app.core.platform.PendingIntentCompat
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.services.CallAndroidService
import im.vector.app.core.utils.startNotificationChannelSettingsIntent
import im.vector.app.features.MainActivity
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.service.CallHeadsUpActionReceiver
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.threads.ThreadsActivity
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.troubleshoot.TestNotificationReceiver
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationUtils @Inject constructor(
        private val context: Context,
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val clock: Clock,
        private val actionIds: NotificationActionIds,
        private val buildMeta: BuildMeta,
) {

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
         * IDs for channels
         * ========================================================================================== */

        // on devices >= android O, we need to define a channel for each notifications
        private const val LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID = "LISTEN_FOR_EVENTS_NOTIFICATION_CHANNEL_ID"

        private const val NOISY_NOTIFICATION_CHANNEL_ID = "DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID"

        const val SILENT_NOTIFICATION_CHANNEL_ID = "DEFAULT_SILENT_NOTIFICATION_CHANNEL_ID_V2"
        private const val CALL_NOTIFICATION_CHANNEL_ID = "CALL_NOTIFICATION_CHANNEL_ID_V2"

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
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
    fun createNotificationChannels() {
        if (!supportNotificationChannels()) {
            return
        }

        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)

        // Migration - the noisy channel was deleted and recreated when sound preference was changed (id was DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID_BASE
        // + currentTimeMillis).
        // Now the sound can only be change directly in system settings, so for app upgrading we are deleting this former channel
        // Starting from this version the channel will not be dynamic
        for (channel in notificationManager.notificationChannels) {
            val channelId = channel.id
            val legacyBaseName = "DEFAULT_NOISY_NOTIFICATION_CHANNEL_ID_BASE"
            if (channelId.startsWith(legacyBaseName)) {
                notificationManager.deleteNotificationChannel(channelId)
            }
        }
        // Migration - Remove deprecated channels
        for (channelId in listOf("DEFAULT_SILENT_NOTIFICATION_CHANNEL_ID", "CALL_NOTIFICATION_CHANNEL_ID")) {
            notificationManager.getNotificationChannel(channelId)?.let {
                notificationManager.deleteNotificationChannel(channelId)
            }
        }

        /**
         * Default notification importance: shows everywhere, makes noise, but does not visually
         * intrude.
         */
        notificationManager.createNotificationChannel(NotificationChannel(
                NOISY_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(CommonStrings.notification_noisy_notifications).ifEmpty { "Noisy notifications" },
                NotificationManager.IMPORTANCE_DEFAULT
        )
                .apply {
                    description = stringProvider.getString(CommonStrings.notification_noisy_notifications)
                    enableVibration(true)
                    enableLights(true)
                    lightColor = accentColor
                })

        /**
         * Low notification importance: shows everywhere, but is not intrusive.
         */
        notificationManager.createNotificationChannel(NotificationChannel(
                SILENT_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(CommonStrings.notification_silent_notifications).ifEmpty { "Silent notifications" },
                NotificationManager.IMPORTANCE_LOW
        )
                .apply {
                    description = stringProvider.getString(CommonStrings.notification_silent_notifications)
                    setSound(null, null)
                    enableLights(true)
                    lightColor = accentColor
                })

        notificationManager.createNotificationChannel(NotificationChannel(
                LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(CommonStrings.notification_listening_for_events).ifEmpty { "Listening for events" },
                NotificationManager.IMPORTANCE_MIN
        )
                .apply {
                    description = stringProvider.getString(CommonStrings.notification_listening_for_events)
                    setSound(null, null)
                    setShowBadge(false)
                })

        notificationManager.createNotificationChannel(NotificationChannel(
                CALL_NOTIFICATION_CHANNEL_ID,
                stringProvider.getString(CommonStrings.call).ifEmpty { "Call" },
                NotificationManager.IMPORTANCE_HIGH
        )
                .apply {
                    description = stringProvider.getString(CommonStrings.call)
                    setSound(null, null)
                    enableLights(true)
                    lightColor = accentColor
                })
    }

    fun getChannel(channelId: String): NotificationChannel? {
        return notificationManager.getNotificationChannel(channelId)
    }

    /**
     * Build a polling thread listener notification.
     *
     * @param subTitleResId subtitle string resource Id of the notification
     * @param withProgress true to show indeterminate progress on the notification
     * @return the polling thread listener notification
     */
    fun buildForegroundServiceNotification(@StringRes subTitleResId: Int, withProgress: Boolean = true): Notification {
        // build the pending intent go to the home screen if this is clicked.
        val i = HomeActivity.newIntent(context, firstStartMainActivity = false)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val mainIntent = MainActivity.getIntentWithNextIntent(context, i)
        val pi = PendingIntent.getActivity(context, 0, mainIntent, PendingIntentCompat.FLAG_IMMUTABLE)

        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)

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
                        .getMethod(
                                "setLatestEventInfo",
                                Context::class.java,
                                CharSequence::class.java,
                                CharSequence::class.java,
                                PendingIntent::class.java
                        )
                deprecatedMethod.invoke(notification, context, buildMeta.applicationName, stringProvider.getString(subTitleResId), pi)
            } catch (ex: Exception) {
                Timber.e(ex, "## buildNotification(): Exception - setLatestEventInfo() Msg=")
            }
        }
        return notification
    }

    fun getChannelForIncomingCall(fromBg: Boolean): NotificationChannel? {
        val notificationChannel = if (fromBg) CALL_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID
        return getChannel(notificationChannel)
    }

    /**
     * Build an incoming call notification.
     * This notification starts the VectorHomeActivity which is in charge of centralizing the incoming call flow.
     *
     * @param call information about the call
     * @param title title of the notification
     * @param fromBg true if the app is in background when posting the notification
     * @return the call notification.
     */
    fun buildIncomingCallNotification(
            call: WebRtcCall,
            title: String,
            fromBg: Boolean
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        val notificationChannel = if (fromBg) CALL_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID
        val builder = NotificationCompat.Builder(context, notificationChannel)
                .setContentTitle(ensureTitleNotEmpty(title))
                .apply {
                    if (call.mxCall.isVideoCall) {
                        setContentText(stringProvider.getString(CommonStrings.incoming_video_call))
                        setSmallIcon(R.drawable.ic_call_answer_video)
                    } else {
                        setContentText(stringProvider.getString(CommonStrings.incoming_voice_call))
                        setSmallIcon(R.drawable.ic_call_answer)
                    }
                }
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setLights(accentColor, 500, 500)
                .setOngoing(true)

        val contentIntent = VectorCallActivity.newIntent(
                context = context,
                call = call,
                mode = VectorCallActivity.INCOMING_RINGING
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = createIgnoredUri(call.callId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
                context,
                clock.epochMillis().toInt(),
                contentIntent,
                PendingIntentCompat.FLAG_IMMUTABLE
        )

        val answerCallPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(HomeActivity.newIntent(context, firstStartMainActivity = false))
                .addNextIntent(
                        VectorCallActivity.newIntent(
                                context = context,
                                call = call,
                                mode = VectorCallActivity.INCOMING_ACCEPT
                        )
                )
                .getPendingIntent(clock.epochMillis().toInt(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE)

        val rejectCallPendingIntent = buildRejectCallPendingIntent(call.callId)

        builder.addAction(
                NotificationCompat.Action(
                        IconCompat.createWithResource(context, R.drawable.ic_call_hangup)
                                .setTint(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError)),
                        getActionText(CommonStrings.call_notification_reject, com.google.android.material.R.attr.colorError),
                        rejectCallPendingIntent
                )
        )

        builder.addAction(
                NotificationCompat.Action(
                        R.drawable.ic_call_answer,
                        getActionText(CommonStrings.call_notification_answer, com.google.android.material.R.attr.colorPrimary),
                        answerCallPendingIntent
                )
        )
        if (fromBg) {
            // Compat: Display the incoming call notification on the lock screen
            builder.priority = NotificationCompat.PRIORITY_HIGH
            builder.setFullScreenIntent(contentPendingIntent, true)
        }
        return builder.build()
    }

    fun buildOutgoingRingingCallNotification(
            call: WebRtcCall,
            title: String
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        val builder = NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(ensureTitleNotEmpty(title))
                .apply {
                    setContentText(stringProvider.getString(CommonStrings.call_ringing))
                    if (call.mxCall.isVideoCall) {
                        setSmallIcon(R.drawable.ic_call_answer_video)
                    } else {
                        setSmallIcon(R.drawable.ic_call_answer)
                    }
                }
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setLights(accentColor, 500, 500)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setOngoing(true)

        val contentIntent = VectorCallActivity.newIntent(
                context = context,
                call = call,
                mode = null
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = createIgnoredUri(call.callId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
                context,
                clock.epochMillis().toInt(),
                contentIntent,
                PendingIntentCompat.FLAG_IMMUTABLE
        )

        val rejectCallPendingIntent = buildRejectCallPendingIntent(call.callId)

        builder.addAction(
                NotificationCompat.Action(
                        IconCompat.createWithResource(context, R.drawable.ic_call_hangup)
                                .setTint(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError)),
                        getActionText(CommonStrings.call_notification_hangup, com.google.android.material.R.attr.colorError),
                        rejectCallPendingIntent
                )
        )
        builder.setContentIntent(contentPendingIntent)

        return builder.build()
    }

    /**
     * Build a pending call notification.
     *
     * @param call information about the call
     * @param title title of the notification
     * @return the call notification.
     */
    fun buildPendingCallNotification(
            call: WebRtcCall,
            title: String
    ): Notification {
        val builder = NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(ensureTitleNotEmpty(title))
                .apply {
                    if (call.mxCall.isVideoCall) {
                        setContentText(stringProvider.getString(CommonStrings.video_call_in_progress))
                        setSmallIcon(R.drawable.ic_call_answer_video)
                    } else {
                        setContentText(stringProvider.getString(CommonStrings.call_in_progress))
                        setSmallIcon(R.drawable.ic_call_answer)
                    }
                }
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_CALL)

        val rejectCallPendingIntent = buildRejectCallPendingIntent(call.callId)

        builder.addAction(
                NotificationCompat.Action(
                        IconCompat.createWithResource(context, R.drawable.ic_call_hangup)
                                .setTint(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError)),
                        getActionText(CommonStrings.call_notification_hangup, com.google.android.material.R.attr.colorError),
                        rejectCallPendingIntent
                )
        )

        val contentPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(HomeActivity.newIntent(context, firstStartMainActivity = false))
                .addNextIntent(VectorCallActivity.newIntent(context, call, null))
                .getPendingIntent(clock.epochMillis().toInt(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE)

        builder.setContentIntent(contentPendingIntent)

        return builder.build()
    }

    private fun buildRejectCallPendingIntent(callId: String): PendingIntent {
        val rejectCallActionReceiver = Intent(context, CallHeadsUpActionReceiver::class.java).apply {
            putExtra(CallHeadsUpActionReceiver.EXTRA_CALL_ID, callId)
            putExtra(CallHeadsUpActionReceiver.EXTRA_CALL_ACTION_KEY, CallHeadsUpActionReceiver.CALL_ACTION_REJECT)
        }
        return PendingIntent.getBroadcast(
                context,
                clock.epochMillis().toInt(),
                rejectCallActionReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
        )
    }

    /**
     * Build a temporary (because service will be stopped just after) notification for the CallService, when a call is ended.
     */
    fun buildCallEndedNotification(isVideoCall: Boolean): Notification {
        return NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(CommonStrings.call_ended))
                .apply {
                    if (isVideoCall) {
                        setSmallIcon(R.drawable.ic_call_answer_video)
                    } else {
                        setSmallIcon(R.drawable.ic_call_answer)
                    }
                }
                .setTimeoutAfter(1)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build()
    }

    /**
     * Build notification for the CallService, when a call is missed.
     */
    fun buildCallMissedNotification(callInformation: CallAndroidService.CallInformation): Notification {
        val builder = NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(callInformation.opponentMatrixItem?.getBestName() ?: callInformation.opponentUserId)
                .apply {
                    if (callInformation.isVideoCall) {
                        setContentText(stringProvider.getQuantityString(CommonPlurals.missed_video_call, 1, 1))
                        setSmallIcon(R.drawable.ic_missed_video_call)
                    } else {
                        setContentText(stringProvider.getQuantityString(CommonPlurals.missed_audio_call, 1, 1))
                        setSmallIcon(R.drawable.ic_missed_voice_call)
                    }
                }
                .setShowWhen(true)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)

        val contentPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(HomeActivity.newIntent(context, firstStartMainActivity = false))
                .addNextIntent(RoomDetailActivity.newIntent(context, TimelineArgs(callInformation.nativeRoomId), true))
                .getPendingIntent(clock.epochMillis().toInt(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE)

        builder.setContentIntent(contentPendingIntent)
        return builder.build()
    }

    /**
     * Creates a notification that indicates the application is capturing the screen.
     */
    fun buildScreenSharingNotification(): Notification {
        return NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(CommonStrings.screen_sharing_notification_title))
                .setContentText(stringProvider.getString(CommonStrings.screen_sharing_notification_description))
                .setSmallIcon(R.drawable.ic_share_screen)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(buildOpenHomePendingIntentForSummary())
                .build()
    }

    /**
     * Creates a notification indicating that the microphone is currently being accessed by the application.
     */
    fun buildMicrophoneAccessNotification(): Notification {
        return NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(CommonStrings.microphone_in_use_title))
                .setSmallIcon(R.drawable.ic_call_answer)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build()
    }

    /**
     * Creates a notification that indicates the application is initializing.
     */
    fun buildStartAppNotification(): Notification {
        return NotificationCompat.Builder(context, LISTENING_FOR_EVENTS_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(CommonStrings.updating_your_data))
                .setSmallIcon(R.drawable.sync)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
    }

    fun buildDownloadFileNotification(uri: Uri, fileName: String, mimeType: String): Notification {
        return NotificationCompat.Builder(context, SILENT_NOTIFICATION_CHANNEL_ID)
                .setGroup(buildMeta.applicationName)
                .setSmallIcon(R.drawable.ic_download)
                .setContentText(stringProvider.getString(CommonStrings.downloaded_file, fileName))
                .setAutoCancel(true)
                .apply {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    PendingIntent.getActivity(
                            context,
                            clock.epochMillis().toInt(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                    ).let {
                        setContentIntent(it)
                    }
                }
                .build()
    }

    /**
     * Build a notification for a Room.
     */
    fun buildMessagesListNotification(
            messageStyle: NotificationCompat.MessagingStyle,
            roomInfo: RoomEventGroupInfo,
            threadId: String?,
            largeIcon: Bitmap?,
            lastMessageTimestamp: Long,
            senderDisplayNameForReplyCompat: String?,
            tickerText: String
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val openIntent = when {
            threadId != null && vectorPreferences.areThreadMessagesEnabled() -> buildOpenThreadIntent(roomInfo, threadId)
            else -> buildOpenRoomIntent(roomInfo.roomId)
        }

        val smallIcon = R.drawable.ic_notification

        val channelID = if (roomInfo.shouldBing) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID
        return NotificationCompat.Builder(context, channelID)
                .setOnlyAlertOnce(roomInfo.isUpdated)
                .setWhen(lastMessageTimestamp)
                // MESSAGING_STYLE sets title and content for API 16 and above devices.
                .setStyle(messageStyle)
                // A category allows groups of notifications to be ranked and filtered – per user or system settings.
                // For example, alarm notifications should display before promo notifications, or message from known contact
                // that can be displayed in not disturb mode if white listed (the later will need compat28.x)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                // ID of the corresponding shortcut, for conversation features under API 30+
                .setShortcutId(roomInfo.roomId)
                // Title for API < 16 devices.
                .setContentTitle(roomInfo.roomDisplayName)
                // Content for API < 16 devices.
                .setContentText(stringProvider.getString(CommonStrings.notification_new_messages))
                // Number of new notifications for API <24 (M and below) devices.
                .setSubText(
                        stringProvider.getQuantityString(CommonPlurals.room_new_messages_notification, messageStyle.messages.size, messageStyle.messages.size)
                )
                // Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. But we want a custom grouping, so we specify the groupID
                // TODO Group should be current user display name
                .setGroup(buildMeta.applicationName)
                // In order to avoid notification making sound twice (due to the summary notification)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                .setSmallIcon(smallIcon)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(accentColor)
                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .apply {
                    if (roomInfo.shouldBing) {
                        // Compat
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        priority = NotificationCompat.PRIORITY_LOW
                    }

                    // Add actions and notification intents
                    // Mark room as read
                    val markRoomReadIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    markRoomReadIntent.action = actionIds.markRoomRead
                    markRoomReadIntent.data = createIgnoredUri(roomInfo.roomId)
                    markRoomReadIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomInfo.roomId)
                    val markRoomReadPendingIntent = PendingIntent.getBroadcast(
                            context,
                            clock.epochMillis().toInt(),
                            markRoomReadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                    )

                    NotificationCompat.Action.Builder(
                            R.drawable.ic_material_done_all_white,
                            stringProvider.getString(CommonStrings.action_mark_room_read), markRoomReadPendingIntent
                    )
                            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                            .setShowsUserInterface(false)
                            .build()
                            .let { addAction(it) }

                    // Quick reply
                    if (!roomInfo.hasSmartReplyError) {
                        buildQuickReplyIntent(roomInfo.roomId, threadId, senderDisplayNameForReplyCompat)?.let { replyPendingIntent ->
                            val remoteInput = RemoteInput.Builder(NotificationBroadcastReceiver.KEY_TEXT_REPLY)
                                    .setLabel(stringProvider.getString(CommonStrings.action_quick_reply))
                                    .build()
                            NotificationCompat.Action.Builder(
                                    R.drawable.vector_notification_quick_reply,
                                    stringProvider.getString(CommonStrings.action_quick_reply), replyPendingIntent
                            )
                                    .addRemoteInput(remoteInput)
                                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                                    .setShowsUserInterface(false)
                                    .build()
                                    .let { addAction(it) }
                        }
                    }

                    if (openIntent != null) {
                        setContentIntent(openIntent)
                    }

                    if (largeIcon != null) {
                        setLargeIcon(largeIcon)
                    }

                    val intent = Intent(context, NotificationBroadcastReceiver::class.java)
                    intent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomInfo.roomId)
                    intent.action = actionIds.dismissRoom
                    val pendingIntent = PendingIntent.getBroadcast(
                            context.applicationContext,
                            clock.epochMillis().toInt(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                    )
                    setDeleteIntent(pendingIntent)
                }
                .setTicker(tickerText)
                .build()
    }

    fun buildRoomInvitationNotification(
            inviteNotifiableEvent: InviteNotifiableEvent,
            matrixId: String
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val smallIcon = R.drawable.ic_notification

        val channelID = if (inviteNotifiableEvent.noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID

        return NotificationCompat.Builder(context, channelID)
                .setOnlyAlertOnce(true)
                .setContentTitle(inviteNotifiableEvent.roomName ?: buildMeta.applicationName)
                .setContentText(inviteNotifiableEvent.description)
                .setGroup(buildMeta.applicationName)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                .setSmallIcon(smallIcon)
                .setColor(accentColor)
                .apply {
                    val roomId = inviteNotifiableEvent.roomId
                    // offer to type a quick reject button
                    val rejectIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    rejectIntent.action = actionIds.reject
                    rejectIntent.data = createIgnoredUri("$roomId&$matrixId")
                    rejectIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
                    val rejectIntentPendingIntent = PendingIntent.getBroadcast(
                            context,
                            clock.epochMillis().toInt(),
                            rejectIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                    )

                    addAction(
                            R.drawable.vector_notification_reject_invitation,
                            stringProvider.getString(CommonStrings.action_reject),
                            rejectIntentPendingIntent
                    )

                    // offer to type a quick accept button
                    val joinIntent = Intent(context, NotificationBroadcastReceiver::class.java)
                    joinIntent.action = actionIds.join
                    joinIntent.data = createIgnoredUri("$roomId&$matrixId")
                    joinIntent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
                    val joinIntentPendingIntent = PendingIntent.getBroadcast(
                            context,
                            clock.epochMillis().toInt(),
                            joinIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                    )
                    addAction(
                            R.drawable.vector_notification_accept_invitation,
                            stringProvider.getString(CommonStrings.action_join),
                            joinIntentPendingIntent
                    )

                    val contentIntent = HomeActivity.newIntent(
                            context,
                            firstStartMainActivity = true,
                            inviteNotificationRoomId = inviteNotifiableEvent.roomId
                    )
                    contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    // pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
                    contentIntent.data = createIgnoredUri(inviteNotifiableEvent.eventId)
                    setContentIntent(PendingIntent.getActivity(context, 0, contentIntent, PendingIntentCompat.FLAG_IMMUTABLE))

                    if (inviteNotifiableEvent.noisy) {
                        // Compat
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

    fun buildSimpleEventNotification(
            simpleNotifiableEvent: SimpleNotifiableEvent,
            matrixId: String
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        // Build the pending intent for when the notification is clicked
        val smallIcon = R.drawable.ic_notification

        val channelID = if (simpleNotifiableEvent.noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID

        return NotificationCompat.Builder(context, channelID)
                .setOnlyAlertOnce(true)
                .setContentTitle(buildMeta.applicationName)
                .setContentText(simpleNotifiableEvent.description)
                .setGroup(buildMeta.applicationName)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                .setSmallIcon(smallIcon)
                .setColor(accentColor)
                .setAutoCancel(true)
                .apply {
                    val contentIntent = HomeActivity.newIntent(context, firstStartMainActivity = true)
                    contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    // pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
                    contentIntent.data = createIgnoredUri(simpleNotifiableEvent.eventId)
                    setContentIntent(PendingIntent.getActivity(context, 0, contentIntent, PendingIntentCompat.FLAG_IMMUTABLE))

                    if (simpleNotifiableEvent.noisy) {
                        // Compat
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
        val roomIntentTap = RoomDetailActivity.newIntent(context, TimelineArgs(roomId = roomId, switchToParentSpace = true), true)
        roomIntentTap.action = actionIds.tapToView
        // pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
        roomIntentTap.data = createIgnoredUri("openRoom?$roomId")

        // Recreate the back stack
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(HomeActivity.newIntent(context, firstStartMainActivity = false))
                .addNextIntent(roomIntentTap)
                .getPendingIntent(
                        clock.epochMillis().toInt(),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                )
    }

    private fun buildOpenThreadIntent(roomInfo: RoomEventGroupInfo, threadId: String?): PendingIntent? {
        val threadTimelineArgs = ThreadTimelineArgs(
                startsThread = false,
                roomId = roomInfo.roomId,
                rootThreadEventId = threadId,
                showKeyboard = false,
                displayName = roomInfo.roomDisplayName,
                avatarUrl = null,
                roomEncryptionTrustLevel = null,
        )
        val threadIntentTap = ThreadsActivity.newIntent(
                context = context,
                threadTimelineArgs = threadTimelineArgs,
                threadListArgs = null,
                firstStartMainActivity = true,
        )
        threadIntentTap.action = actionIds.tapToView
        // pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
        threadIntentTap.data = createIgnoredUri("openThread?$threadId")

        val roomIntent = RoomDetailActivity.newIntent(
                context = context,
                timelineArgs = TimelineArgs(
                        roomId = roomInfo.roomId,
                        switchToParentSpace = true
                ),
                firstStartMainActivity = false
        )
        // Recreate the back stack
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(HomeActivity.newIntent(context, firstStartMainActivity = false))
                .addNextIntentWithParentStack(roomIntent)
                .addNextIntent(threadIntentTap)
                .getPendingIntent(
                        clock.epochMillis().toInt(),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                )
    }

    private fun buildOpenHomePendingIntentForSummary(): PendingIntent {
        val intent = HomeActivity.newIntent(context, firstStartMainActivity = false, clearNotification = true)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.data = createIgnoredUri("tapSummary")
        val mainIntent = MainActivity.getIntentWithNextIntent(context, intent)
        return PendingIntent.getActivity(
                context,
                Random.nextInt(1000),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
        )
    }

    /*
        Direct reply is new in Android N, and Android already handles the UI, so the right pending intent
        here will ideally be a Service/IntentService (for a long running background task) or a BroadcastReceiver,
         which runs on the UI thread. It also works without unlocking, making the process really fluid for the user.
        However, for Android devices running Marshmallow and below (API level 23 and below),
        it will be more appropriate to use an activity. Since you have to provide your own UI.
     */
    private fun buildQuickReplyIntent(roomId: String, threadId: String?, senderName: String?): PendingIntent? {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(context, NotificationBroadcastReceiver::class.java)
            intent.action = actionIds.smartReply
            intent.data = createIgnoredUri(roomId)
            intent.putExtra(NotificationBroadcastReceiver.KEY_ROOM_ID, roomId)
            threadId?.let {
                intent.putExtra(NotificationBroadcastReceiver.KEY_THREAD_ID, it)
            }

            return PendingIntent.getBroadcast(
                    context,
                    clock.epochMillis().toInt(),
                    intent,
                    // PendingIntents attached to actions with remote inputs must be mutable
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_MUTABLE
            )
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
                quickReplyIntent.data = createIgnoredUri($roomId")
                return PendingIntent.getActivity(context, 0, quickReplyIntent, PendingIntentCompat.FLAG_IMMUTABLE)
            }
             */
        }
        return null
    }

    // // Number of new notifications for API <24 (M and below) devices.
    /**
     * Build the summary notification.
     */
    fun buildSummaryListNotification(
            style: NotificationCompat.InboxStyle?,
            compatSummary: String,
            noisy: Boolean,
            lastMessageTimestamp: Long
    ): Notification {
        val accentColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color)
        val smallIcon = R.drawable.ic_notification

        return NotificationCompat.Builder(context, if (noisy) NOISY_NOTIFICATION_CHANNEL_ID else SILENT_NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                // used in compat < N, after summary is built based on child notifications
                .setWhen(lastMessageTimestamp)
                .setStyle(style)
                .setContentTitle(buildMeta.applicationName)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSmallIcon(smallIcon)
                // set content text to support devices running API level < 24
                .setContentText(compatSummary)
                .setGroup(buildMeta.applicationName)
                // set this notification as the summary for the group
                .setGroupSummary(true)
                .setColor(accentColor)
                .apply {
                    if (noisy) {
                        // Compat
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        vectorPreferences.getNotificationRingTone()?.let {
                            setSound(it)
                        }
                        setLights(accentColor, 500, 500)
                    } else {
                        // compat
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                }
                .setContentIntent(buildOpenHomePendingIntentForSummary())
                .setDeleteIntent(getDismissSummaryPendingIntent())
                .build()
    }

    private fun getDismissSummaryPendingIntent(): PendingIntent {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        intent.action = actionIds.dismissSummary
        intent.data = createIgnoredUri("deleteSummary")
        return PendingIntent.getBroadcast(
                context.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
        )
    }

    fun showNotificationMessage(tag: String?, id: Int, notification: Notification) {
        notificationManager.notify(tag, id, notification)
    }

    fun cancelNotificationMessage(tag: String?, id: Int) {
        notificationManager.cancel(tag, id)
    }

    /**
     * Cancel the foreground notification service.
     */
    fun cancelNotificationForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID_FOREGROUND_SERVICE)
    }

    /**
     * Cancel all the notification.
     */
    fun cancelAllNotifications() {
        // Keep this try catch (reported by GA)
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Timber.e(e, "## cancelAllNotifications() failed")
        }
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun displayDiagnosticNotification() {
        val testActionIntent = Intent(context, TestNotificationReceiver::class.java)
        testActionIntent.action = actionIds.diagnostic
        val testPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                testActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
        )

        notificationManager.notify(
                "DIAGNOSTIC",
                888,
                NotificationCompat.Builder(context, NOISY_NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(buildMeta.applicationName)
                        .setContentText(stringProvider.getString(CommonStrings.settings_troubleshoot_test_push_notification_content))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(getBitmap(context, im.vector.lib.ui.styles.R.drawable.element_logo_green))
                        .setColor(ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.notification_accent_color))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .setAutoCancel(true)
                        .setContentIntent(testPendingIntent)
                        .build()
        )
    }

    private fun getBitmap(context: Context, @DrawableRes drawableRes: Int): Bitmap? {
        val drawable = ResourcesCompat.getDrawable(context.resources, drawableRes, null) ?: return null
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Return true it the user has enabled the do not disturb mode.
     */
    fun isDoNotDisturbModeOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        // We cannot use NotificationManagerCompat here.
        val setting = context.getSystemService<NotificationManager>()!!.currentInterruptionFilter

        return setting == NotificationManager.INTERRUPTION_FILTER_NONE ||
                setting == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    private fun getActionText(@StringRes stringRes: Int, @AttrRes colorRes: Int): Spannable {
        return SpannableString(context.getText(stringRes)).apply {
            val foregroundColorSpan = ForegroundColorSpan(ThemeUtils.getColor(context, colorRes))
            setSpan(foregroundColorSpan, 0, length, 0)
        }
    }

    private fun ensureTitleNotEmpty(title: String?): CharSequence {
        if (title.isNullOrBlank()) {
            return buildMeta.applicationName
        }

        return title
    }
}
