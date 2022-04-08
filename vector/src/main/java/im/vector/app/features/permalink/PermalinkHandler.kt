/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.permalink

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.isIgnored
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.core.utils.toast
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomdirectory.roompreview.RoomPreviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.timeline.isRootThread
import javax.inject.Inject

class PermalinkHandler @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                           private val userPreferencesProvider: UserPreferencesProvider,
                                           private val navigator: Navigator) {

    suspend fun launch(
            context: Context,
            deepLink: String?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        val uri = deepLink?.let { Uri.parse(it) }
        return launch(context, uri, navigationInterceptor, buildTask)
    }

    suspend fun launch(
            context: Context,
            deepLink: Uri?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        return when {
            deepLink == null                                    -> false
            deepLink.isIgnored()                                -> true
            !isPermalinkSupported(context, deepLink.toString()) -> false
            else                                                -> {
                tryOrNull {
                    withContext(Dispatchers.Default) {
                        val permalinkData = PermalinkParser.parse(deepLink)
                        handlePermalink(permalinkData, deepLink, context, navigationInterceptor, buildTask)
                    }
                } ?: false
            }
        }
    }

    private suspend fun handlePermalink(
            permalinkData: PermalinkData,
            rawLink: Uri,
            context: Context,
            navigationInterceptor: NavigationInterceptor?,
            buildTask: Boolean
    ): Boolean {
        return when (permalinkData) {
            is PermalinkData.RoomLink            -> {
                val roomId = permalinkData.getRoomId()
                val session = activeSessionHolder.getSafeActiveSession()

                val rootThreadEventId = permalinkData.eventId?.let { eventId ->
                    val room = roomId?.let { session?.getRoom(it) }

                    val rootThreadEventId = room?.getTimelineEvent(eventId)?.root?.getRootThreadEventId()
                    rootThreadEventId ?: if (room?.getTimelineEvent(eventId)?.isRootThread() == true) {
                        eventId
                    } else {
                        null
                    }
                }
                openRoom(
                        navigationInterceptor,
                        context = context,
                        roomId = roomId,
                        permalinkData = permalinkData,
                        rawLink = rawLink,
                        buildTask = buildTask,
                        rootThreadEventId = rootThreadEventId
                )
                true
            }
            is PermalinkData.GroupLink           -> {
                navigator.openGroupDetail(permalinkData.groupId, context, buildTask)
                true
            }
            is PermalinkData.UserLink            -> {
                if (navigationInterceptor?.navToMemberProfile(permalinkData.userId, rawLink) != true) {
                    navigator.openRoomMemberProfile(userId = permalinkData.userId, roomId = null, context = context, buildTask = buildTask)
                }
                true
            }
            is PermalinkData.FallbackLink        -> {
                false
            }
            is PermalinkData.RoomEmailInviteLink -> {
                val data = RoomPreviewData(
                        roomId = permalinkData.roomId,
                        roomName = permalinkData.roomName,
                        avatarUrl = permalinkData.roomAvatarUrl,
                        fromEmailInvite = permalinkData,
                        roomType = permalinkData.roomType
                )
                navigator.openRoomPreview(context, data)
                true
            }
        }
    }

    private fun isPermalinkSupported(context: Context, url: String): Boolean {
        return url.startsWith(PermalinkService.MATRIX_TO_URL_BASE) ||
                context.resources.getStringArray(R.array.permalink_supported_hosts)
                        .any { url.toUri().host == it }
    }

    private suspend fun PermalinkData.RoomLink.getRoomId(): String? {
        val session = activeSessionHolder.getSafeActiveSession()
        return if (isRoomAlias && session != null) {
            val roomIdByAlias = session.getRoomIdByAlias(roomIdOrAlias, true)
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
     * Open room either joined, or not
     */
    private fun openRoom(
            navigationInterceptor: NavigationInterceptor?,
            context: Context,
            roomId: String?,
            permalinkData: PermalinkData.RoomLink,
            rawLink: Uri,
            buildTask: Boolean,
            rootThreadEventId: String? = null
    ) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        if (roomId == null) {
            context.toast(R.string.room_error_not_found)
            return
        }
        val roomSummary = session.getRoomSummary(roomId)
        val membership = roomSummary?.membership
        val eventId = permalinkData.eventId
//        val roomAlias = permalinkData.getRoomAliasOrNull()
        val isSpace = roomSummary?.roomType == RoomType.SPACE
        return when {
            membership == Membership.BAN     -> context.toast(R.string.error_opening_banned_room)
            membership?.isActive().orFalse() -> {
                if (!isSpace && membership == Membership.JOIN) {
                    // If it's a room you're in, let's just open it, you can tap back if needed
                    navigationInterceptor.openJoinedRoomScreen(buildTask, roomId, eventId, rawLink, context, rootThreadEventId, roomSummary)
                } else {
                    // maybe open space preview navigator.openSpacePreview(context, roomId)? if already joined?
                    navigator.openMatrixToBottomSheet(context, rawLink.toString())
                }
            }
            else                             -> {
                // XXX this could trigger another server load
                navigator.openMatrixToBottomSheet(context, rawLink.toString())
            }
        }
    }

    private fun NavigationInterceptor?.openJoinedRoomScreen(buildTask: Boolean,
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
                        rootThreadEventId = rootThreadEventId)
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
     * Return true if the navigation has been intercepted
     */
    fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri? = null, rootThreadEventId: String? = null): Boolean {
        return false
    }

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        return false
    }
}
