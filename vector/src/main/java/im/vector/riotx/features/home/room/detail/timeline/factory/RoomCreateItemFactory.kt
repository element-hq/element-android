/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home.room.detail.timeline.factory

import im.vector.matrix.android.api.permalinks.PermalinkFactory
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.create.RoomCreateContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.item.RoomCreateItem
import im.vector.riotx.features.home.room.detail.timeline.item.RoomCreateItem_
import me.gujun.android.span.span
import javax.inject.Inject

class RoomCreateItemFactory @Inject constructor(private val colorProvider: ColorProvider,
                                                private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent, callback: TimelineEventController.Callback?): RoomCreateItem? {
        val createRoomContent = event.root.getClearContent().toModel<RoomCreateContent>()
                                ?: return null
        val predecessorId = createRoomContent.predecessor?.roomId ?: return null
        val roomLink = PermalinkFactory.createPermalink(predecessorId) ?: return null
        val text = span {
            +stringProvider.getString(R.string.room_tombstone_continuation_description)
            +"\n"
            span(stringProvider.getString(R.string.room_tombstone_predecessor_link)) {
                textDecorationLine = "underline"
                onClick = { callback?.onRoomCreateLinkClicked(roomLink) }
            }
        }
        return RoomCreateItem_()
                .text(text)
    }


}