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
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.navigation.Navigator
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.util.Optional
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
        return when (val permalinkData = PermalinkParser.parse(deepLink)) {
            is PermalinkData.RoomLink     -> {
                permalinkData.getRoomId()
                        .observeOn(AndroidSchedulers.mainThread())
                        .map {
                            val roomId = it.getOrNull()
                            if (navigationInterceptor?.navToRoom(roomId, permalinkData.eventId) != true) {
                                openRoom(context, roomId, permalinkData.eventId, buildTask)
                            }
                            true
                        }
            }
            is PermalinkData.GroupLink    -> {
                navigator.openGroupDetail(permalinkData.groupId, context, buildTask)
                Single.just(true)
            }
            is PermalinkData.UserLink     -> {
                if (navigationInterceptor?.navToMemberProfile(permalinkData.userId) != true) {
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
            // At the moment we are not fetching on the server as we don't handle not join room
            session.rx().getRoomIdByAlias(roomIdOrAlias, false).subscribeOn(Schedulers.io())
        } else {
            Single.just(Optional.from(roomIdOrAlias))
        }
    }

    /**
     * Open room either joined, or not
     */
    private fun openRoom(context: Context, roomId: String?, eventId: String?, buildTask: Boolean) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        return if (roomId != null && session.getRoom(roomId) != null) {
            navigator.openRoom(context, roomId, eventId, buildTask)
        } else {
            navigator.openNotJoinedRoom(context, roomId, eventId, buildTask)
        }
    }
}

interface NavigationInterceptor {

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToRoom(roomId: String?, eventId: String? = null): Boolean {
        return false
    }

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToMemberProfile(userId: String): Boolean {
        return false
    }
}
