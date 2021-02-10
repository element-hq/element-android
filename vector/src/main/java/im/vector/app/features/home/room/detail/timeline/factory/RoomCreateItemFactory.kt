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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.RoomCreateItem_
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomCreateItemFactory @Inject constructor(private val stringProvider: StringProvider,
                                                private val userPreferencesProvider: UserPreferencesProvider,
                                                private val session: Session,
                                                private val noticeItemFactory: NoticeItemFactory) {

    fun create(event: TimelineEvent, callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {
        val createRoomContent = event.root.getClearContent().toModel<RoomCreateContent>() ?: return null
        val predecessorId = createRoomContent.predecessor?.roomId ?: return defaultRendering(event, callback)
        val roomLink = session.permalinkService().createRoomPermalink(predecessorId) ?: return null
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

    private fun defaultRendering(event: TimelineEvent, callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {
        return if (userPreferencesProvider.shouldShowHiddenEvents()) {
            noticeItemFactory.create(event, false, callback)
        } else {
            null
        }
    }
}
