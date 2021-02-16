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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.item.NoticeItem
import im.vector.app.features.home.room.detail.timeline.item.NoticeItem_
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class NoticeItemFactory @Inject constructor(private val eventFormatter: NoticeEventFormatter,
                                            private val avatarRenderer: AvatarRenderer,
                                            private val informationDataFactory: MessageInformationDataFactory,
                                            private val avatarSizeProvider: AvatarSizeProvider) {

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): NoticeItem? {
        val formattedText = eventFormatter.format(event) ?: return null
        val informationData = informationDataFactory.create(event, null)
        val attributes = NoticeItem.Attributes(
                avatarRenderer = avatarRenderer,
                informationData = informationData,
                noticeText = formattedText,
                itemLongClickListener = { view ->
                    callback?.onEventLongClicked(informationData, null, view) ?: false
                },
                readReceiptsCallback = callback,
                avatarClickListener = { callback?.onAvatarClicked(informationData) }
        )
        return NoticeItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(highlight)
                .attributes(attributes)
    }
}
