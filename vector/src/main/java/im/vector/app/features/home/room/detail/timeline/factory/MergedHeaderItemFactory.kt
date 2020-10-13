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

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.prevOrNull
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MergedTimelineEventVisibilityStateChangedListener
import im.vector.app.features.home.room.detail.timeline.helper.RoomSummaryHolder
import im.vector.app.features.home.room.detail.timeline.helper.canBeMerged
import im.vector.app.features.home.room.detail.timeline.helper.isRoomConfiguration
import im.vector.app.features.home.room.detail.timeline.helper.prevSameTypeEvents
import im.vector.app.features.home.room.detail.timeline.item.BasedMergedItem
import im.vector.app.features.home.room.detail.timeline.item.MergedMembershipEventsItem
import im.vector.app.features.home.room.detail.timeline.item.MergedMembershipEventsItem_
import im.vector.app.features.home.room.detail.timeline.item.MergedRoomCreationItem
import im.vector.app.features.home.room.detail.timeline.item.MergedRoomCreationItem_
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.model.event.EncryptionEventContent
import javax.inject.Inject

class MergedHeaderItemFactory @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                                  private val avatarRenderer: AvatarRenderer,
                                                  private val avatarSizeProvider: AvatarSizeProvider,
                                                  private val roomSummaryHolder: RoomSummaryHolder) {

    private val collapsedEventIds = linkedSetOf<Long>()
    private val mergeItemCollapseStates = HashMap<Long, Boolean>()

    /**
     * @param nextEvent is an older event than event
     * @param items all known items, sorted from newer event to oldest event
     */
    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               items: List<TimelineEvent>,
               addDaySeparator: Boolean,
               currentPosition: Int,
               eventIdToHighlight: String?,
               callback: TimelineEventController.Callback?,
               requestModelBuild: () -> Unit)
            : BasedMergedItem<*>? {
        return if (nextEvent?.root?.getClearType() == EventType.STATE_ROOM_CREATE
                && event.isRoomConfiguration(nextEvent.root.getClearContent()?.toModel<RoomCreateContent>()?.creator)) {
            // It's the first item before room.create
            // Collapse all room configuration events
            buildRoomCreationMergedSummary(currentPosition, items, event, eventIdToHighlight, requestModelBuild, callback)
        } else if (!event.canBeMerged() || (nextEvent?.root?.getClearType() == event.root.getClearType() && !addDaySeparator)) {
            null
        } else {
            buildMembershipEventsMergedSummary(currentPosition, items, event, eventIdToHighlight, requestModelBuild, callback)
        }
    }

    private fun isDirectRoom() = roomSummaryHolder.roomSummary?.isDirect.orFalse()

    private fun buildMembershipEventsMergedSummary(currentPosition: Int,
                                                   items: List<TimelineEvent>,
                                                   event: TimelineEvent,
                                                   eventIdToHighlight: String?,
                                                   requestModelBuild: () -> Unit,
                                                   callback: TimelineEventController.Callback?): MergedMembershipEventsItem_? {
        val prevSameTypeEvents = items.prevSameTypeEvents(currentPosition, 2)
        return if (prevSameTypeEvents.isEmpty()) {
            null
        } else {
            var highlighted = false
            val mergedEvents = (prevSameTypeEvents + listOf(event)).asReversed()
            val mergedData = ArrayList<BasedMergedItem.Data>(mergedEvents.size)
            mergedEvents.forEach { mergedEvent ->
                if (!highlighted && mergedEvent.root.eventId == eventIdToHighlight) {
                    highlighted = true
                }
                val data = BasedMergedItem.Data(
                        userId = mergedEvent.root.senderId ?: "",
                        avatarUrl = mergedEvent.senderInfo.avatarUrl,
                        memberName = mergedEvent.senderInfo.disambiguatedDisplayName,
                        localId = mergedEvent.localId,
                        eventId = mergedEvent.root.eventId ?: "",
                        isDirectRoom = isDirectRoom()
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
            val attributes = MergedMembershipEventsItem.Attributes(
                    isCollapsed = isCollapsed,
                    mergeData = mergedData,
                    avatarRenderer = avatarRenderer,
                    onCollapsedStateChanged = {
                        mergeItemCollapseStates[event.localId] = it
                        requestModelBuild()
                    },
                    readReceiptsCallback = callback
            )
            MergedMembershipEventsItem_()
                    .id(mergeId)
                    .leftGuideline(avatarSizeProvider.leftGuideline)
                    .highlighted(isCollapsed && highlighted)
                    .attributes(attributes)
                    .also {
                        it.setOnVisibilityStateChanged(MergedTimelineEventVisibilityStateChangedListener(callback, mergedEvents))
                    }
        }
    }

    private fun buildRoomCreationMergedSummary(currentPosition: Int,
                                               items: List<TimelineEvent>,
                                               event: TimelineEvent,
                                               eventIdToHighlight: String?,
                                               requestModelBuild: () -> Unit,
                                               callback: TimelineEventController.Callback?): MergedRoomCreationItem_? {
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
        return if (mergedEvents.size > 2) {
            var highlighted = false
            val mergedData = ArrayList<BasedMergedItem.Data>(mergedEvents.size)
            mergedEvents.reversed()
                    .forEach { mergedEvent ->
                        if (!highlighted && mergedEvent.root.eventId == eventIdToHighlight) {
                            highlighted = true
                        }
                        val data = BasedMergedItem.Data(
                                userId = mergedEvent.root.senderId ?: "",
                                avatarUrl = mergedEvent.senderInfo.avatarUrl,
                                memberName = mergedEvent.senderInfo.disambiguatedDisplayName,
                                localId = mergedEvent.localId,
                                eventId = mergedEvent.root.eventId ?: "",
                                isDirectRoom = isDirectRoom()
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
                    readReceiptsCallback = callback,
                    currentUserId = activeSessionHolder.getSafeActiveSession()?.myUserId ?: ""
            )
            MergedRoomCreationItem_()
                    .id(mergeId)
                    .leftGuideline(avatarSizeProvider.leftGuideline)
                    .highlighted(isCollapsed && highlighted)
                    .attributes(attributes)
                    .also {
                        it.setOnVisibilityStateChanged(MergedTimelineEventVisibilityStateChangedListener(callback, mergedEvents))
                    }
        } else null
    }

    fun isCollapsed(localId: Long): Boolean {
        return collapsedEventIds.contains(localId)
    }
}
