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
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.utils.toast
import im.vector.app.features.navigation.Navigator
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.rx.rx
import javax.inject.Inject

class PermalinkHandler @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                           private val navigator: Navigator) {

    fun launch(
            context: Context,
            deepLink: String?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Single<Boolean> {
        val uri = deepLink?.let { Uri.parse(it) }
        return launch(context, uri, navigationInterceptor, buildTask)
    }

    fun launch(
            context: Context,
            deepLink: Uri?,
            navigationInterceptor: NavigationInterceptor? = null,
            buildTask: Boolean = false
    ): Single<Boolean> {
        if (deepLink == null) {
            return Single.just(false)
        }
        return Single
                .fromCallable {
                    PermalinkParser.parse(deepLink)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { permalinkData ->
                    handlePermalink(permalinkData, deepLink, context, navigationInterceptor, buildTask)
                }
                .onErrorReturnItem(false)
    }

    private fun handlePermalink(
            permalinkData: PermalinkData,
            rawLink: Uri,
            context: Context,
            navigationInterceptor: NavigationInterceptor?,
            buildTask: Boolean
    ): Single<Boolean> {
        return when (permalinkData) {
            is PermalinkData.RoomLink -> {
                permalinkData.getRoomId()
                        .observeOn(AndroidSchedulers.mainThread())
                        .map {
                            val roomId = it.getOrNull()
                            if (navigationInterceptor?.navToRoom(roomId, permalinkData.eventId, rawLink) != true) {
                                openRoom(
                                        context = context,
                                        roomId = roomId,
                                        permalinkData = permalinkData,
                                        rawLink = rawLink,
                                        buildTask = buildTask
                                )
                            }
                            true
                        }
            }
            is PermalinkData.GroupLink -> {
                navigator.openGroupDetail(permalinkData.groupId, context, buildTask)
                Single.just(true)
            }
            is PermalinkData.UserLink -> {
                if (navigationInterceptor?.navToMemberProfile(permalinkData.userId, rawLink) != true) {
                    navigator.openRoomMemberProfile(userId = permalinkData.userId, roomId = null, context = context, buildTask = buildTask)
                }
                Single.just(true)
            }
            is PermalinkData.FallbackLink -> {
                Single.just(false)
            }
        }
    }

    private fun PermalinkData.RoomLink.getRoomId(): Single<Optional<String>> {
        val session = activeSessionHolder.getSafeActiveSession()
        return if (isRoomAlias && session != null) {
            session.rx()
                    .getRoomIdByAlias(roomIdOrAlias, true)
                    .map { it.getOrNull()?.roomId.toOptional() }
                    .subscribeOn(Schedulers.io())
        } else {
            Single.just(Optional.from(roomIdOrAlias))
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
            context: Context,
            roomId: String?,
            permalinkData: PermalinkData.RoomLink,
            rawLink: Uri,
            buildTask: Boolean
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
                    navigator.openRoom(context, roomId, eventId, buildTask)
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
}

interface NavigationInterceptor {

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri? = null): Boolean {
        return false
    }

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToMemberProfile(userId: String, deepLink: Uri): Boolean {
        return false
    }
}
