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

package im.vector.riotx.features.permalink

import android.content.Context
import android.net.Uri
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.features.navigation.Navigator
import javax.inject.Inject

class PermalinkHandler @Inject constructor(private val session: Session,
                                           private val navigator: Navigator) {

    fun launch(
            context: Context,
            deepLink: String?,
            navigateToRoomInterceptor: NavigateToRoomInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        val uri = deepLink?.let { Uri.parse(it) }
        return launch(context, uri, navigateToRoomInterceptor, buildTask)
    }

    fun launch(
            context: Context,
            deepLink: Uri?,
            navigateToRoomInterceptor: NavigateToRoomInterceptor? = null,
            buildTask: Boolean = false
    ): Boolean {
        if (deepLink == null) {
            return false
        }

        return when (val permalinkData = PermalinkParser.parse(deepLink)) {
            is PermalinkData.EventLink    -> {
                if (navigateToRoomInterceptor?.navToRoom(permalinkData.roomIdOrAlias, permalinkData.eventId) != true) {
                    openRoom(context, permalinkData.roomIdOrAlias, permalinkData.eventId, buildTask)
                }
                true
            }
            is PermalinkData.RoomLink     -> {
                if (navigateToRoomInterceptor?.navToRoom(permalinkData.roomIdOrAlias) != true) {
                    openRoom(context, permalinkData.roomIdOrAlias, null, buildTask)
                }

                true
            }
            is PermalinkData.GroupLink    -> {
                navigator.openGroupDetail(permalinkData.groupId, context, buildTask)
                false
            }
            is PermalinkData.UserLink     -> {
                navigator.openUserDetail(permalinkData.userId, context, buildTask)
                true
            }
            is PermalinkData.FallbackLink -> {
                false
            }
        }
    }

    /**
     * Open room either joined, or not unknown
     */
    private fun openRoom(context: Context, roomIdOrAlias: String, eventId: String? = null, buildTask: Boolean) {
        if (session.getRoom(roomIdOrAlias) != null) {
            navigator.openRoom(context, roomIdOrAlias, eventId, buildTask)
        } else {
            navigator.openNotJoinedRoom(context, roomIdOrAlias, eventId, buildTask)
        }
    }
}

interface NavigateToRoomInterceptor {

    /**
     * Return true if the navigation has been intercepted
     */
    fun navToRoom(roomId: String, eventId: String? = null): Boolean
}
