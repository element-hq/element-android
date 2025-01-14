/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.permalink

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.isIgnored
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.core.utils.toast
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomdirectory.roompreview.RoomPreviewData
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.timeline.isRootThread
import javax.inject.Inject

class PermalinkHandler @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val navigator: Navigator
) {

    suspend fun launch(
            fragmentActivity: FragmentActivity,
            deepLink: String?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        val uri = deepLink?.let { Uri.parse(it) }
        return launch(fragmentActivity, uri, navigationInterceptor, buildTask)
    }

    suspend fun launch(
            fragmentActivity: FragmentActivity,
            deepLink: Uri?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        val supportedHosts = fragmentActivity.resources.getStringArray(im.vector.app.config.R.array.permalink_supported_hosts)
        return when {
            deepLink == null -> false
            deepLink.isIgnored() -> true
            !activeSessionHolder.getSafeActiveSession()?.permalinkService()?.isPermalinkSupported(supportedHosts, deepLink.toString()).orFalse() -> false
            else -> {
                tryOrNull {
                    withContext(Dispatchers.Default) {
                        val permalinkData = PermalinkParser.parse(deepLink)
                        handlePermalink(permalinkData, deepLink, fragmentActivity, navigationInterceptor, buildTask)
                    }
                } ?: false
            }
        }
    }

    private suspend fun handlePermalink(
            permalinkData: PermalinkData,
            rawLink: Uri,
            fragmentActivity: FragmentActivity,
            navigationInterceptor: NavigationInterceptor?,
            buildTask: Boolean
    ): Boolean {
        return when (permalinkData) {
            is PermalinkData.RoomLink -> handleRoomLink(permalinkData, rawLink, fragmentActivity, navigationInterceptor, buildTask)
            is PermalinkData.UserLink -> handleUserLink(permalinkData, rawLink, fragmentActivity, navigationInterceptor, buildTask)
            is PermalinkData.FallbackLink -> handleFallbackLink(permalinkData, fragmentActivity)
            is PermalinkData.RoomEmailInviteLink -> handleRoomInviteLink(permalinkData, fragmentActivity)
        }
    }

    private suspend fun handleRoomLink(
            permalinkData: PermalinkData.RoomLink,
            rawLink: Uri,
            fragmentActivity: FragmentActivity,
            navigationInterceptor: NavigationInterceptor?,
            buildTask: Boolean
    ): Boolean {
        val roomId = permalinkData.getRoomId()
        val session = activeSessionHolder.getSafeActiveSession()

        val rootThreadEventId = permalinkData.eventId?.let { eventId ->
            val room = roomId?.let { session?.getRoom(it) }
            val event = room?.getTimelineEvent(eventId)
            event?.root?.getRootThreadEventId() ?: eventId.takeIf { event?.isRootThread() == true }
        }
        openRoom(
                navigationInterceptor,
                fragmentActivity = fragmentActivity,
                roomId = roomId,
                permalinkData = permalinkData,
                rawLink = rawLink,
                buildTask = buildTask,
                rootThreadEventId = rootThreadEventId
        )
        return true
    }

    private fun handleUserLink(
            permalinkData: PermalinkData.UserLink,
            rawLink: Uri,
            context: Context,
            navigationInterceptor: NavigationInterceptor?,
            buildTask: Boolean
    ): Boolean {
        if (navigationInterceptor?.navToMemberProfile(permalinkData.userId, rawLink) != true) {
            navigator.openRoomMemberProfile(userId = permalinkData.userId, roomId = null, context = context, buildTask = buildTask)
        }
        return true
    }

    private fun handleRoomInviteLink(
            permalinkData: PermalinkData.RoomEmailInviteLink,
            context: Context
    ): Boolean {
        val data = RoomPreviewData(
                roomId = permalinkData.roomId,
                roomName = permalinkData.roomName,
                avatarUrl = permalinkData.roomAvatarUrl,
                fromEmailInvite = permalinkData,
                roomType = permalinkData.roomType
        )
        navigator.openRoomPreview(context, data)
        return true
    }

    private suspend fun handleFallbackLink(
            permalinkData: PermalinkData.FallbackLink,
            context: Context
    ): Boolean {
        return if (permalinkData.isLegacyGroupLink) {
            withContext(Dispatchers.Main) {
                navigator.showGroupsUnsupportedWarning(context)
            }
            true
        } else {
            false
        }
    }

    private suspend fun PermalinkData.RoomLink.getRoomId(): String? {
        val session = activeSessionHolder.getSafeActiveSession()
        return if (isRoomAlias && session != null) {
            val roomIdByAlias = session.roomService().getRoomIdByAlias(roomIdOrAlias, true)
            roomIdByAlias.getOrNull()?.roomId
        } else {
            roomIdOrAlias
        }
    }

    private fun PermalinkData.RoomLink.getRoomAliasOrNull(): String? {
        return if (isRoomAlias) {
            roomIdOrAlias
        } else {
            null
        }
    }

    /**
     * Open room either joined, or not.
     */
    private fun openRoom(
            navigationInterceptor: NavigationInterceptor?,
            fragmentActivity: FragmentActivity,
            roomId: String?,
            permalinkData: PermalinkData.RoomLink,
            rawLink: Uri,
            buildTask: Boolean,
            rootThreadEventId: String? = null
    ) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        if (roomId == null) {
            fragmentActivity.toast(CommonStrings.room_error_not_found)
            return
        }
        val roomSummary = session.getRoomSummary(roomId)
        val membership = roomSummary?.membership
        val eventId = permalinkData.eventId
//        val roomAlias = permalinkData.getRoomAliasOrNull()
        val isSpace = roomSummary?.roomType == RoomType.SPACE
        return when {
            membership == Membership.BAN -> fragmentActivity.toast(CommonStrings.error_opening_banned_room)
            membership?.isActive().orFalse() -> {
                if (!isSpace && membership == Membership.JOIN) {
                    // If it's a room you're in, let's just open it, you can tap back if needed
                    navigationInterceptor.openJoinedRoomScreen(buildTask, roomId, eventId, rawLink, fragmentActivity, rootThreadEventId, roomSummary)
                } else {
                    // maybe open space preview navigator.openSpacePreview(fragmentActivity, roomId)? if already joined?
                    navigator.openMatrixToBottomSheet(fragmentActivity, rawLink.toString(), OriginOfMatrixTo.LINK)
                }
            }
            else -> {
                // XXX this could trigger another server load
                navigator.openMatrixToBottomSheet(fragmentActivity, rawLink.toString(), OriginOfMatrixTo.LINK)
            }
        }
    }

    private fun NavigationInterceptor?.openJoinedRoomScreen(
            buildTask: Boolean,
            roomId: String,
            eventId: String?,
            rawLink: Uri,
            context: Context,
            rootThreadEventId: String?,
            roomSummary: RoomSummary
    ) {
        if (this?.navToRoom(roomId, eventId, rawLink, rootThreadEventId) != true) {
            if (rootThreadEventId != null && userPreferencesProvider.areThreadMessagesEnabled()) {
                val threadTimelineArgs = ThreadTimelineArgs(
                        roomId = roomId,
                        displayName = roomSummary.displayName,
                        avatarUrl = roomSummary.avatarUrl,
                        roomEncryptionTrustLevel = roomSummary.roomEncryptionTrustLevel,
                        rootThreadEventId = rootThreadEventId
                )
                navigator.openThread(context, threadTimelineArgs, eventId)
            } else {
                navigator.openRoom(context, roomId, eventId, buildTask)
            }
        }
    }

    companion object {
        const val MATRIX_TO_CUSTOM_SCHEME_URL_BASE = "element://"
        const val ROOM_LINK_PREFIX = "${MATRIX_TO_CUSTOM_SCHEME_URL_BASE}room/"
        const val USER_LINK_PREFIX = "${MATRIX_TO_CUSTOM_SCHEME_URL_BASE}user/"
    }
}

interface NavigationInterceptor {

    /**
     * Return true if the navigation has been intercepted.
     */
    fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri? = null, rootThreadEventId: String? = null): Boolean {
        return false
    }

    /**
     * Return true if the navigation has been intercepted.
     */
    fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        return false
    }
}
