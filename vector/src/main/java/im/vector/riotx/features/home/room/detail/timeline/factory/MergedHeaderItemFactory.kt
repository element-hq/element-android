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
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.*
import im.vector.riotx.features.home.room.detail.timeline.item.MergedHeaderItem
import im.vector.riotx.features.home.room.detail.timeline.item.MergedHeaderItem_
import javax.inject.Inject

class MergedHeaderItemFactory @Inject constructor(private val sessionHolder: ActiveSessionHolder,
                                                  private val avatarRenderer: AvatarRenderer,
                                                  private val avatarSizeProvider: AvatarSizeProvider) {

    private val collapsedEventIds = linkedSetOf<Long>()
    private val mergeItemCollapseStates = HashMap<Long, Boolean>()

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               items: List<TimelineEvent>,
               addDaySeparator: Boolean,
               currentPosition: Int,
               eventIdToHighlight: String?,
               callback: TimelineEventController.Callback?,
               requestModelBuild: () -> Unit)
            : MergedHeaderItem? {
        return if (!event.canBeMerged() || (nextEvent?.root?.getClearType() == event.root.getClearType() && !addDaySeparator)) {
            null
        } else {
            val prevSameTypeEvents = items.prevSameTypeEvents(currentPosition, 2)
            if (prevSameTypeEvents.isEmpty()) {
                null
            } else {
                var highlighted = false
                val mergedEvents = (prevSameTypeEvents + listOf(event)).asReversed()
                val mergedData = ArrayList<MergedHeaderItem.Data>(mergedEvents.size)
                mergedEvents.forEach { mergedEvent ->
                    if (!highlighted && mergedEvent.root.eventId == eventIdToHighlight) {
                        highlighted = true
                    }
                    val senderAvatar = mergedEvent.senderAvatar
                    val senderName = mergedEvent.getDisambiguatedDisplayName()
                    val data = MergedHeaderItem.Data(
                            userId = mergedEvent.root.senderId ?: "",
                            avatarUrl = senderAvatar,
                            memberName = senderName,
                            localId = mergedEvent.localId,
                            eventId = mergedEvent.root.eventId ?: ""
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
                val attributes = MergedHeaderItem.Attributes(
                        isCollapsed = isCollapsed,
                        mergeData = mergedData,
                        avatarRenderer = avatarRenderer,
                        onCollapsedStateChanged = {
                            mergeItemCollapseStates[event.localId] = it
                            requestModelBuild()
                        },
                        readReceiptsCallback = callback
                )
                MergedHeaderItem_()
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

    fun isCollapsed(localId: Long): Boolean {
        return collapsedEventIds.contains(localId)
    }
}
