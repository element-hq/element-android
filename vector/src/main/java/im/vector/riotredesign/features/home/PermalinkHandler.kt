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

package im.vector.riotredesign.features.home

import android.content.Context
import android.net.Uri
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.riotredesign.core.utils.openUrlInExternalBrowser
import im.vector.riotredesign.features.navigation.Navigator

class PermalinkHandler(private val navigator: Navigator) {

    fun launch(context: Context, deepLink: String?) {
        val uri = deepLink?.let { Uri.parse(it) }
        launch(context, uri)
    }

    fun launch(context: Context, deepLink: Uri?) {
        if (deepLink == null) {
            return
        }
        when (val permalinkData = PermalinkParser.parse(deepLink)) {
            is PermalinkData.EventLink    -> {
                navigator.openRoom(context, permalinkData.roomIdOrAlias, permalinkData.eventId)
            }
            is PermalinkData.RoomLink     -> {
                navigator.openRoom(context, permalinkData.roomIdOrAlias)
            }
            is PermalinkData.GroupLink    -> {
                navigator.openGroupDetail(permalinkData.groupId, context)
            }
            is PermalinkData.UserLink     -> {
                navigator.openUserDetail(permalinkData.userId, context)
            }
            is PermalinkData.FallbackLink -> {
                openUrlInExternalBrowser(context, permalinkData.uri)
            }
        }
    }
}