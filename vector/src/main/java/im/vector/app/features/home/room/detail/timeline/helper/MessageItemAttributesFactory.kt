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
package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.EmojiCompatFontProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import javax.inject.Inject

class MessageItemAttributesFactory @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val messageColorProvider: MessageColorProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val emojiCompatFontProvider: EmojiCompatFontProvider) {

    fun create(messageContent: Any?,
               informationData: MessageInformationData,
               callback: TimelineEventController.Callback?): AbsMessageItem.Attributes {
        return AbsMessageItem.Attributes(
                avatarSize = avatarSizeProvider.avatarSize,
                informationData = informationData,
                avatarRenderer = avatarRenderer,
                messageColorProvider = messageColorProvider,
                itemLongClickListener = { view ->
                    callback?.onEventLongClicked(informationData, messageContent, view) ?: false
                },
                itemClickListener = { view ->
                    callback?.onEventCellClicked(informationData, messageContent, view)
                },
                memberClickListener = {
                    callback?.onMemberNameClicked(informationData)
                },
                reactionPillCallback = callback,
                avatarCallback = callback,
                readReceiptsCallback = callback,
                emojiTypeFace = emojiCompatFontProvider.typeface
        )
    }
}
