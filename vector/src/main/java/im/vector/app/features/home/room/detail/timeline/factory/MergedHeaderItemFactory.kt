/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.prevOrNull
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MergedTimelineEventVisibilityStateChangedListener
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityHelper
import im.vector.app.features.home.room.detail.timeline.helper.isRoomConfiguration
import im.vector.app.features.home.room.detail.timeline.item.BasedMergedItem
import im.vector.app.features.home.room.detail.timeline.item.MergedRoomCreationItem
import im.vector.app.features.home.room.detail.timeline.item.MergedRoomCreationItem_
import im.vector.app.features.home.room.detail.timeline.item.MergedSimilarEventsItem
import im.vector.app.features.home.room.detail.timeline.item.MergedSimilarEventsItem_
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.lib.strings.CommonPlurals
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class MergedHeaderItemFactory @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val avatarRenderer: AvatarRenderer,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val timelineEventVisibilityHelper: TimelineEventVisibilityHelper
) {

    private val mergeableEventTypes = listOf(EventType.STATE_ROOM_MEMBER, EventType.STATE_ROOM_SERVER_ACL)
    private val collapsedEventIds = linkedSetOf<Long>()
    private val mergeItemCollapseStates = HashMap<Long, Boolean>()

    /**
     * @param event the main timeline event
     * @param nextEvent is an older event than event
     * @param items all known items, sorted from newer event to oldest event
     * @param partialState partial state data
     * @param addDaySeparator true to add a day separator
     * @param currentPosition the current position
     * @param eventIdToHighlight if not null the event which has to be highlighted
     * @param callback callback for user event
     * @param requestModelBuild lambda to let the built Item request a model build when the collapse state is changed
     */
    fun create(
            event: TimelineEvent,
            nextEvent: TimelineEvent?,
            items: List<TimelineEvent>,
            partialState: TimelineEventController.PartialState,
            addDaySeparator: Boolean,
            currentPosition: Int,
            eventIdToHighlight: String?,
            callback: TimelineEventController.Callback?,
            requestModelBuild: () -> Unit
    ): BasedMergedItem<*>? {
        return when {
            isStartOfRoomCreationSummary(event, nextEvent) ->
                buildRoomCreationMergedSummary(currentPosition, items, partialState, event, eventIdToHighlight, requestModelBuild, callback)
            isStartOfSameTypeEventsSummary(event, nextEvent, addDaySeparator) ->
                buildSameTypeEventsMergedSummary(currentPosition, items, partialState, event, eventIdToHighlight, requestModelBuild, callback)
            isStartOfRedactedEventsSummary(event, items, currentPosition, partialState, addDaySeparator) ->
                buildRedactedEventsMergedSummary(currentPosition, items, partialState, event, eventIdToHighlight, requestModelBuild, callback)
            else -> null
        }
    }

    /**
     * @param event the main timeline event
     * @param nextEvent is an older event than event
     */
    private fun isStartOfRoomCreationSummary(
            event: TimelineEvent,
            nextEvent: TimelineEvent?,
    ): Boolean {
        // It's the first item before room.create
        // Collapse all room configuration events
        return nextEvent?.root?.getClearType() == EventType.STATE_ROOM_CREATE &&
                event.isRoomConfiguration(nextEvent.root.getClearContent()?.toModel<RoomCreateContent>()?.creator)
    }

    /**
     * @param event the main timeline event
     * @param nextEvent is an older event than event
     * @param addDaySeparator true to add a day separator
     */
    private fun isStartOfSameTypeEventsSummary(
            event: TimelineEvent,
            nextEvent: TimelineEvent?,
            addDaySeparator: Boolean,
    ): Boolean {
        return event.root.getClearType() in mergeableEventTypes &&
                (nextEvent?.root?.getClearType() != event.root.getClearType() || addDaySeparator)
    }

    /**
     * @param event the main timeline event
     * @param items all known items, sorted from newer event to oldest event
     * @param currentPosition the current position
     * @param partialState partial state data
     * @param addDaySeparator true to add a day separator
     */
    private fun isStartOfRedactedEventsSummary(
            event: TimelineEvent,
            items: List<TimelineEvent>,
            currentPosition: Int,
            partialState: TimelineEventController.PartialState,
            addDaySeparator: Boolean,
    ): Boolean {
        val nextDisplayableEvent = items.subList(currentPosition + 1, items.size).firstOrNull {
            timelineEventVisibilityHelper.shouldShowEvent(
                    timelineEvent = it,
                    highlightedEventId = partialState.highlightedEventId,
                    isFromThreadTimeline = partialState.isFromThreadTimeline(),
                    rootThreadEventId = partialState.rootThreadEventId
            )
        }
        return event.root.isRedacted() && (nextDisplayableEvent?.root?.isRedacted() == false || addDaySeparator)
    }

    private fun buildSameTypeEventsMergedSummary(
            currentPosition: Int,
            items: List<TimelineEvent>,
            partialState: TimelineEventController.PartialState,
            event: TimelineEvent,
            eventIdToHighlight: String?,
            requestModelBuild: () -> Unit,
            callback: TimelineEventController.Callback?
    ): MergedSimilarEventsItem_? {
        val mergedEvents = timelineEventVisibilityHelper.prevSameTypeEvents(
                items,
                currentPosition,
                MIN_NUMBER_OF_MERGED_EVENTS,
                eventIdToHighlight,
                partialState.rootThreadEventId,
                partialState.isFromThreadTimeline()
        )
        return buildSimilarEventsMergedSummary(mergedEvents, partialState, event, eventIdToHighlight, requestModelBuild, callback)
    }

    private fun buildRedactedEventsMergedSummary(
            currentPosition: Int,
            items: List<TimelineEvent>,
            partialState: TimelineEventController.PartialState,
            event: TimelineEvent,
            eventIdToHighlight: String?,
            requestModelBuild: () -> Unit,
            callback: TimelineEventController.Callback?
    ): MergedSimilarEventsItem_? {
        val mergedEvents = timelineEventVisibilityHelper.prevRedactedEvents(
                items,
                currentPosition,
                MIN_NUMBER_OF_MERGED_EVENTS,
                eventIdToHighlight,
                partialState.rootThreadEventId,
                partialState.isFromThreadTimeline()
        )
        return buildSimilarEventsMergedSummary(mergedEvents, partialState, event, eventIdToHighlight, requestModelBuild, callback)
    }

    private fun buildSimilarEventsMergedSummary(
            mergedEvents: List<TimelineEvent>,
            partialState: TimelineEventController.PartialState,
            event: TimelineEvent,
            eventIdToHighlight: String?,
            requestModelBuild: () -> Unit,
            callback: TimelineEventController.Callback?
    ): MergedSimilarEventsItem_? {
        return if (mergedEvents.isEmpty()) {
            null
        } else {
            var highlighted = false
            val mergedData = ArrayList<BasedMergedItem.Data>(mergedEvents.size)
            mergedEvents.forEach { mergedEvent ->
                if (!highlighted && mergedEvent.root.eventId == eventIdToHighlight) {
                    highlighted = true
                }
                val data = BasedMergedItem.Data(
                        roomId = mergedEvent.root.roomId,
                        userId = mergedEvent.root.senderId ?: "",
                        avatarUrl = mergedEvent.senderInfo.avatarUrl,
                        memberName = mergedEvent.senderInfo.disambiguatedDisplayName,
                        localId = mergedEvent.localId,
                        eventId = mergedEvent.root.eventId ?: "",
                        isDirectRoom = partialState.isDirectRoom()
                )
                mergedData.add(data)
            }
            val mergedEventIds = mergedEvents.map { it.localId }.toSet()
            // We try to find if one of the item id were used as mergeItemCollapseStates key
            // => handle case where paginating from mergeable events and we get more
            val previousCollapseStateKey = mergedEventIds.intersect(mergeItemCollapseStates.keys).firstOrNull()
            val initialCollapseState = mergeItemCollapseStates.remove(previousCollapseStateKey)
                    ?: true
            val isCollapsed = mergeItemCollapseStates.getOrPut(event.localId) { initialCollapseState }
            if (isCollapsed) {
                collapsedEventIds.addAll(mergedEventIds)
            } else {
                collapsedEventIds.removeAll(mergedEventIds)
            }
            val mergeId = mergedEventIds.joinToString(separator = "_") { it.toString() }
            getSummaryTitleResId(event.root)?.let { summaryTitle ->
                val attributes = MergedSimilarEventsItem.Attributes(
                        summaryTitleResId = summaryTitle,
                        isCollapsed = isCollapsed,
                        mergeData = mergedData,
                        avatarRenderer = avatarRenderer,
                        onCollapsedStateChanged = {
                            mergeItemCollapseStates[event.localId] = it
                            requestModelBuild()
                        }
                )
                MergedSimilarEventsItem_()
                        .id(mergeId)
                        .leftGuideline(avatarSizeProvider.leftGuideline)
                        .highlighted(isCollapsed && highlighted)
                        .attributes(attributes)
                        .also {
                            it.setOnVisibilityStateChanged(MergedTimelineEventVisibilityStateChangedListener(callback, mergedEvents))
                        }
            }
        }
    }

    private fun getSummaryTitleResId(event: Event): Int? {
        val type = event.getClearType()
        return when {
            type == EventType.STATE_ROOM_MEMBER -> CommonPlurals.membership_changes
            type == EventType.STATE_ROOM_SERVER_ACL -> CommonPlurals.notice_room_server_acl_changes
            event.isRedacted() -> CommonPlurals.room_removed_messages
            else -> null
        }
    }

    private fun buildRoomCreationMergedSummary(
            currentPosition: Int,
            items: List<TimelineEvent>,
            partialState: TimelineEventController.PartialState,
            event: TimelineEvent,
            eventIdToHighlight: String?,
            requestModelBuild: () -> Unit,
            callback: TimelineEventController.Callback?
    ): MergedRoomCreationItem_? {
        var prevEvent = items.prevOrNull(currentPosition)
        var tmpPos = currentPosition - 1
        val mergedEvents = mutableListOf(event)
        var hasEncryption = false
        var encryptionAlgorithm: String? = null
        while (prevEvent != null && prevEvent.isRoomConfiguration(null)) {
            if (prevEvent.root.isStateEvent() && prevEvent.root.getClearType() == EventType.STATE_ROOM_ENCRYPTION) {
                hasEncryption = true
                encryptionAlgorithm = prevEvent.root.getClearContent()?.toModel<EncryptionEventContent>()?.algorithm
            }
            mergedEvents.add(prevEvent)
            tmpPos--
            prevEvent = items.getOrNull(tmpPos)
        }
        return if (mergedEvents.size > MIN_NUMBER_OF_MERGED_EVENTS) {
            var highlighted = false
            val mergedData = ArrayList<BasedMergedItem.Data>(mergedEvents.size)
            mergedEvents.reversed()
                    .forEach { mergedEvent ->
                        if (!highlighted && mergedEvent.root.eventId == eventIdToHighlight) {
                            highlighted = true
                        }
                        val data = BasedMergedItem.Data(
                                roomId = mergedEvent.root.roomId,
                                userId = mergedEvent.root.senderId ?: "",
                                avatarUrl = mergedEvent.senderInfo.avatarUrl,
                                memberName = mergedEvent.senderInfo.disambiguatedDisplayName,
                                localId = mergedEvent.localId,
                                eventId = mergedEvent.root.eventId ?: "",
                                isDirectRoom = partialState.isDirectRoom()
                        )
                        mergedData.add(data)
                    }
            val mergedEventIds = mergedEvents.map { it.localId }
            // We try to find if one of the item id were used as mergeItemCollapseStates key
            // => handle case where paginating from mergeable events and we get more
            val previousCollapseStateKey = mergedEventIds.intersect(mergeItemCollapseStates.keys).firstOrNull()
            val initialCollapseState = mergeItemCollapseStates.remove(previousCollapseStateKey)
                    ?: true
            val isCollapsed = mergeItemCollapseStates.getOrPut(event.localId) { initialCollapseState }
            if (isCollapsed) {
                collapsedEventIds.addAll(mergedEventIds)
            } else {
                collapsedEventIds.removeAll(mergedEventIds)
            }
            val mergeId = mergedEventIds.joinToString(separator = "_") { it.toString() }
            val powerLevelsHelper = activeSessionHolder.getSafeActiveSession()?.getRoom(event.roomId)
                    ?.let { it.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)?.content?.toModel<PowerLevelsContent>() }
                    ?.let { PowerLevelsHelper(it) }
            val currentUserId = activeSessionHolder.getSafeActiveSession()?.myUserId ?: ""
            val attributes = MergedRoomCreationItem.Attributes(
                    isCollapsed = isCollapsed,
                    mergeData = mergedData,
                    avatarRenderer = avatarRenderer,
                    onCollapsedStateChanged = {
                        mergeItemCollapseStates[event.localId] = it
                        requestModelBuild()
                    },
                    hasEncryptionEvent = hasEncryption,
                    isEncryptionAlgorithmSecure = encryptionAlgorithm == MXCRYPTO_ALGORITHM_MEGOLM,
                    callback = callback,
                    currentUserId = currentUserId,
                    roomSummary = partialState.roomSummary,
                    canInvite = powerLevelsHelper?.isUserAbleToInvite(currentUserId) ?: false,
                    canChangeAvatar = powerLevelsHelper?.isUserAllowedToSend(currentUserId, true, EventType.STATE_ROOM_AVATAR) ?: false,
                    canChangeTopic = powerLevelsHelper?.isUserAllowedToSend(currentUserId, true, EventType.STATE_ROOM_TOPIC) ?: false,
                    canChangeName = powerLevelsHelper?.isUserAllowedToSend(currentUserId, true, EventType.STATE_ROOM_NAME) ?: false
            )
            MergedRoomCreationItem_()
                    .id(mergeId)
                    .leftGuideline(avatarSizeProvider.leftGuideline)
                    .highlighted(isCollapsed && highlighted)
                    .attributes(attributes)
                    .movementMethod(createLinkMovementMethod(callback))
                    .also {
                        it.setOnVisibilityStateChanged(MergedTimelineEventVisibilityStateChangedListener(callback, mergedEvents))
                    }
        } else null
    }

    private fun TimelineEventController.PartialState.isDirectRoom(): Boolean {
        return roomSummary?.isDirect.orFalse()
    }

    fun isCollapsed(localId: Long): Boolean {
        return collapsedEventIds.contains(localId)
    }

    companion object {
        private const val MIN_NUMBER_OF_MERGED_EVENTS = 2
    }
}
