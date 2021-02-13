/*
 * Copyright 2021 New Vector Ltd
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

import im.vector.app.ActiveSessionDataSource
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.WidgetTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.WidgetTileTimelineItem_
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.widgets.model.WidgetContent
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import javax.inject.Inject

class WidgetItemFactory @Inject constructor(
        private val sp: StringProvider,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val informationDataFactory: MessageInformationDataFactory,
        private val noticeItemFactory: NoticeItemFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val activeSessionDataSource: ActiveSessionDataSource
) {
    private val currentUserId: String?
        get() = activeSessionDataSource.currentValue?.orNull()?.myUserId

    private fun Event.isSentByCurrentUser() = senderId != null && senderId == currentUserId

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {
        val widgetContent: WidgetContent = event.root.getClearContent().toModel() ?: return null
        val previousWidgetContent: WidgetContent? = event.root.resolvedPrevContent().toModel()

        return when (WidgetType.fromString(widgetContent.type ?: previousWidgetContent?.type ?: "")) {
            WidgetType.Jitsi -> createJitsiItem(event, callback, widgetContent, previousWidgetContent)
            // There is lot of other widget types we could improve here
            else             -> noticeItemFactory.create(event, highlight, callback)
        }
    }

    private fun createJitsiItem(timelineEvent: TimelineEvent,
                                callback: TimelineEventController.Callback?,
                                widgetContent: WidgetContent,
                                previousWidgetContent: WidgetContent?): VectorEpoxyModel<*> {
        val informationData = informationDataFactory.create(timelineEvent, null)
        val attributes = messageItemAttributesFactory.create(null, informationData, callback)

        val disambiguatedDisplayName = timelineEvent.senderInfo.disambiguatedDisplayName
        val message = if (widgetContent.isActive()) {
            val widgetName = widgetContent.getHumanName()
            if (previousWidgetContent?.isActive().orFalse()) {
                // Widget has been modified
                if (timelineEvent.root.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_jitsi_modified_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_jitsi_modified, disambiguatedDisplayName, widgetName)
                }
            } else {
                // Widget has been added
                if (timelineEvent.root.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_jitsi_added_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_jitsi_added, disambiguatedDisplayName, widgetName)
                }
            }
        } else {
            // Widget has been removed
            val widgetName = previousWidgetContent?.getHumanName()
            if (timelineEvent.root.isSentByCurrentUser()) {
                sp.getString(R.string.notice_widget_jitsi_removed_by_you, widgetName)
            } else {
                sp.getString(R.string.notice_widget_jitsi_removed, disambiguatedDisplayName, widgetName)
            }
        }

        return WidgetTileTimelineItem_()
                .attributes(
                        WidgetTileTimelineItem.Attributes(
                                title = message,
                                drawableStart = R.drawable.ic_video,
                                informationData = informationData,
                                avatarRenderer = attributes.avatarRenderer,
                                messageColorProvider = attributes.messageColorProvider,
                                itemLongClickListener = attributes.itemLongClickListener,
                                itemClickListener = attributes.itemClickListener,
                                reactionPillCallback = attributes.reactionPillCallback,
                                readReceiptsCallback = attributes.readReceiptsCallback,
                                emojiTypeFace = attributes.emojiTypeFace
                        )
                )
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
