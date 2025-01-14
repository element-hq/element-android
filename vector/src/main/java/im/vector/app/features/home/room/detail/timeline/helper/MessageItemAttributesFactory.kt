/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.EmojiCompatFontProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryEvents
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.threads.ThreadDetails
import javax.inject.Inject

class MessageItemAttributesFactory @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val messageColorProvider: MessageColorProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val stringProvider: StringProvider,
        private val displayableEventFormatter: DisplayableEventFormatter,
        private val preferencesProvider: UserPreferencesProvider,
        private val emojiCompatFontProvider: EmojiCompatFontProvider
) {

    fun create(
            messageContent: Any?,
            informationData: MessageInformationData,
            callback: TimelineEventController.Callback?,
            reactionsSummaryEvents: ReactionsSummaryEvents?,
            threadDetails: ThreadDetails? = null
    ): AbsMessageItem.Attributes {
        return AbsMessageItem.Attributes(
                avatarSize = avatarSizeProvider.avatarSize,
                informationData = informationData,
                avatarRenderer = avatarRenderer,
                messageColorProvider = messageColorProvider,
                itemLongClickListener = { view ->
                    callback?.onEventLongClicked(informationData, messageContent, view) ?: false
                },
                itemClickListener = { view ->
                    callback?.onEventCellClicked(informationData, messageContent, view, threadDetails?.isRootThread ?: false)
                },
                memberClickListener = {
                    callback?.onMemberNameClicked(informationData)
                },
                callback = callback,
                reactionPillCallback = callback,
                avatarCallback = callback,
                threadCallback = callback,
                readReceiptsCallback = callback,
                emojiTypeFace = emojiCompatFontProvider.typeface,
                decryptionErrorMessage = stringProvider.getString(CommonStrings.encrypted_message),
                threadSummaryFormatted = displayableEventFormatter.formatThreadSummary(threadDetails?.threadSummaryLatestEvent).toString(),
                threadDetails = threadDetails,
                reactionsSummaryEvents = reactionsSummaryEvents,
                areThreadMessagesEnabled = preferencesProvider.areThreadMessagesEnabled(),
                autoplayAnimatedImages = preferencesProvider.autoplayAnimatedImages()
        )
    }
}
