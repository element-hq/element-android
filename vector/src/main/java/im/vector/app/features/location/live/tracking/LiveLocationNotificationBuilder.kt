/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.tracking

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import im.vector.app.R
import im.vector.app.core.extensions.createIgnoredUri
import im.vector.app.core.platform.PendingIntentCompat
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.location.live.map.LiveLocationMapViewActivity
import im.vector.app.features.location.live.map.LiveLocationMapViewArgs
import im.vector.app.features.notifications.NotificationActionIds
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveLocationNotificationBuilder @Inject constructor(
        private val context: Context,
        private val stringProvider: StringProvider,
        private val clock: Clock,
        private val actionIds: NotificationActionIds,
) {

    /**
     * Creates a notification that indicates the application is retrieving location even if it is in background or killed.
     * @param roomId the id of the room where a live location is shared
     */
    fun buildLiveLocationSharingNotification(roomId: String): Notification {
        return NotificationCompat.Builder(context, NotificationUtils.SILENT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(stringProvider.getString(CommonStrings.live_location_sharing_notification_title))
                .setContentText(stringProvider.getString(CommonStrings.live_location_sharing_notification_description))
                .setSmallIcon(R.drawable.ic_attachment_live_location_white)
                .setColor(ThemeUtils.getColor(context, android.R.attr.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING)
                .setContentIntent(buildOpenLiveLocationMapIntent(roomId))
                .build()
    }

    private fun buildOpenLiveLocationMapIntent(roomId: String): PendingIntent? {
        val homeIntent = HomeActivity.newIntent(context, firstStartMainActivity = false)
        val roomIntent = RoomDetailActivity.newIntent(context, TimelineArgs(roomId = roomId, switchToParentSpace = true), firstStartMainActivity = false)
        val mapIntent = LiveLocationMapViewActivity.getIntent(
                context = context,
                liveLocationMapViewArgs = LiveLocationMapViewArgs(roomId = roomId),
                firstStartMainActivity = true
        )
        mapIntent.action = actionIds.tapToView
        // pending intent get reused by system, this will mess up the extra params, so put unique info to avoid that
        mapIntent.data = createIgnoredUri("openLiveLocationMap?$roomId")

        // Recreate the back stack
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(homeIntent)
                .addNextIntent(roomIntent)
                .addNextIntent(mapIntent)
                .getPendingIntent(
                        clock.epochMillis().toInt(),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
                )
    }
}
