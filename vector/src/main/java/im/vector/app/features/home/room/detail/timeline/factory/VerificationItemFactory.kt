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
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.StatusTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.StatusTileTimelineItem_
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.VerificationState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationCancelContent
import javax.inject.Inject

/**
 * Can creates verification conclusion items
 * Notice that not all KEY_VERIFICATION_DONE will be displayed in timeline,
 * several checks are made to see if this conclusion is attached to a known request
 */
class VerificationItemFactory @Inject constructor(
        private val messageColorProvider: MessageColorProvider,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val noticeItemFactory: NoticeItemFactory,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val stringProvider: StringProvider,
        private val session: Session
) {

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        if (event.root.eventId == null) return null

        val relContent: MessageRelationContent = event.root.content.toModel()
                ?: event.root.getClearContent().toModel()
                ?: return ignoredConclusion(params)

        if (relContent.relatesTo?.type != RelationType.REFERENCE) return ignoredConclusion(params)
        val refEventId = relContent.relatesTo?.eventId
                ?: return ignoredConclusion(params)

        // If we cannot find the referenced request we do not display the done event
        val refEvent = session.getRoom(event.root.roomId ?: "")?.getTimelineEvent(refEventId)
                ?: return ignoredConclusion(params)

        // If it's not a request ignore this event
        // if (refEvent.root.getClearContent().toModel<MessageVerificationRequestContent>() == null) return ignoredConclusion(event, highlight, callback)

        val referenceInformationData = messageInformationDataFactory.create(TimelineItemFactoryParams(event = refEvent))

        val informationData = messageInformationDataFactory.create(params)
        val attributes = messageItemAttributesFactory.create(null, informationData, params.callback, params.reactionsSummaryEvents)

        when (event.root.getClearType()) {
            EventType.KEY_VERIFICATION_CANCEL -> {
                // Is the request referenced is actually really cancelled?
                val cancelContent = event.root.getClearContent().toModel<MessageVerificationCancelContent>()
                        ?: return ignoredConclusion(params)

                when (safeValueOf(cancelContent.code)) {
                    CancelCode.MismatchedCommitment,
                    CancelCode.MismatchedKeys,
                    CancelCode.MismatchedSas -> {
                        // We should display these bad conclusions
                        return StatusTileTimelineItem_()
                                .attributes(
                                        StatusTileTimelineItem.Attributes(
                                                title = stringProvider.getString(R.string.verification_conclusion_warning),
                                                description = "${informationData.memberName} (${informationData.senderId})",
                                                shieldUIState = StatusTileTimelineItem.ShieldUIState.RED,
                                                informationData = informationData,
                                                avatarRenderer = attributes.avatarRenderer,
                                                messageColorProvider = messageColorProvider,
                                                emojiTypeFace = attributes.emojiTypeFace,
                                                itemClickListener = attributes.itemClickListener,
                                                itemLongClickListener = attributes.itemLongClickListener,
                                                reactionPillCallback = attributes.reactionPillCallback,
                                                readReceiptsCallback = attributes.readReceiptsCallback,
                                                reactionsSummaryEvents = attributes.reactionsSummaryEvents
                                        )
                                )
                                .highlighted(params.isHighlighted)
                                .leftGuideline(avatarSizeProvider.leftGuideline)
                    }
                    else                     -> return ignoredConclusion(params)
                }
            }
            EventType.KEY_VERIFICATION_DONE   -> {
                // Is the request referenced is actually really completed?
                if (referenceInformationData.referencesInfoData?.verificationStatus != VerificationState.DONE) {
                    return ignoredConclusion(params)
                }
                // We only tale the one sent by me

                if (informationData.sentByMe) {
                    // We only display the done sent by the other user, the done send by me is ignored
                    return ignoredConclusion(params)
                }
                return StatusTileTimelineItem_()
                        .attributes(
                                StatusTileTimelineItem.Attributes(
                                        title = stringProvider.getString(R.string.sas_verified),
                                        description = "${informationData.memberName} (${informationData.senderId})",
                                        shieldUIState = StatusTileTimelineItem.ShieldUIState.GREEN,
                                        informationData = informationData,
                                        avatarRenderer = attributes.avatarRenderer,
                                        messageColorProvider = messageColorProvider,
                                        emojiTypeFace = attributes.emojiTypeFace,
                                        itemClickListener = attributes.itemClickListener,
                                        itemLongClickListener = attributes.itemLongClickListener,
                                        reactionPillCallback = attributes.reactionPillCallback,
                                        readReceiptsCallback = attributes.readReceiptsCallback,
                                        reactionsSummaryEvents = attributes.reactionsSummaryEvents
                                )
                        )
                        .highlighted(params.isHighlighted)
                        .leftGuideline(avatarSizeProvider.leftGuideline)
            }
        }
        return null
    }

    private fun ignoredConclusion(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        if (userPreferencesProvider.shouldShowHiddenEvents()) return noticeItemFactory.create(params)
        return null
    }
}
