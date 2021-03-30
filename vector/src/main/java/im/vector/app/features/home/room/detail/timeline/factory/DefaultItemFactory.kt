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

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.item.DefaultItem
import im.vector.app.features.home.room.detail.timeline.item.DefaultItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

class DefaultItemFactory @Inject constructor(private val avatarSizeProvider: AvatarSizeProvider,
                                             private val avatarRenderer: AvatarRenderer,
                                             private val stringProvider: StringProvider,
                                             private val informationDataFactory: MessageInformationDataFactory) {

    fun create(text: String,
               informationData: MessageInformationData,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): DefaultItem {
        val attributes = DefaultItem.Attributes(
                avatarRenderer = avatarRenderer,
                informationData = informationData,
                text = text,
                itemLongClickListener = { view ->
                    callback?.onEventLongClicked(informationData, null, view) ?: false
                }
        )
        return DefaultItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(highlight)
                .attributes(attributes)
    }

    fun create(params: TimelineItemFactoryParams, throwable: Throwable? = null): DefaultItem {
        val event = params.event
        val text = if (throwable == null) {
            stringProvider.getString(R.string.rendering_event_error_type_of_event_not_handled, event.root.getClearType())
        } else {
            stringProvider.getString(R.string.rendering_event_error_exception, event.root.eventId)
        }
        val informationData = informationDataFactory.create(params)
        return create(text, informationData, params.isHighlighted, params.callback)
    }
}
