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

package im.vector.riotx.features.home.room.detail.timeline.factory

import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.riotx.features.home.room.detail.timeline.item.DefaultItem
import im.vector.riotx.features.home.room.detail.timeline.item.DefaultItem_
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

class DefaultItemFactory @Inject constructor(private val avatarSizeProvider: AvatarSizeProvider,
                                             private val avatarRenderer: AvatarRenderer,
                                             private val informationDataFactory: MessageInformationDataFactory) {

    fun create(text: String,
               informationData: MessageInformationData,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): DefaultItem {
        return DefaultItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(highlight)
                .text(text)
                .avatarRenderer(avatarRenderer)
                .informationData(informationData)
                .baseCallback(callback)
                .readReceiptsCallback(callback)
    }

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?,
               exception: Exception? = null): DefaultItem {
        val text = if (exception == null) {
            "${event.root.getClearType()} events are not yet handled"
        } else {
            "an exception occurred when rendering the event ${event.root.eventId}"
        }
        val informationData = informationDataFactory.create(event, null)
        return create(text, informationData, highlight, callback)
    }
}
