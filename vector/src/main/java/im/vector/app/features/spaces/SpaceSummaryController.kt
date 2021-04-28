/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItemHeader
import im.vector.app.features.grouplist.groupSummaryItem
import im.vector.app.features.grouplist.homeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.group
import im.vector.app.space
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceSummaryController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider) : EpoxyController() {

    var callback: Callback? = null
    private var viewState: SpaceListViewState? = null

    init {
        requestModelBuild()
    }

    fun update(viewState: SpaceListViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val nonNullViewState = viewState ?: return
        buildGroupModels(
                nonNullViewState.asyncSpaces(),
                nonNullViewState.selectedGroupingMethod,
                nonNullViewState.rootSpaces,
                nonNullViewState.expandedStates,
                nonNullViewState.homeAggregateCount)

        if (!nonNullViewState.legacyGroups.isNullOrEmpty()) {
            genericFooterItem {
                id("legacy_space")
                text(" ")
            }

            genericItemHeader {
                id("legacy_groups")
                text(stringProvider.getString(R.string.groups_header))
            }

            // add home for communities
            nonNullViewState.myMxItem.invoke()?.let { mxItem ->
                groupSummaryItem {
                    avatarRenderer(avatarRenderer)
                    id("all_communities")
                    matrixItem(mxItem.copy(displayName = stringProvider.getString(R.string.group_all_communities)))
                    selected(nonNullViewState.selectedGroupingMethod is RoomGroupingMethod.ByLegacyGroup
                            && nonNullViewState.selectedGroupingMethod.group() == null)
                    listener { callback?.onGroupSelected(null) }
                }
            }

            nonNullViewState.legacyGroups.forEach { groupSummary ->
                groupSummaryItem {
                    avatarRenderer(avatarRenderer)
                    id(groupSummary.groupId)
                    matrixItem(groupSummary.toMatrixItem())
                    selected(nonNullViewState.selectedGroupingMethod is RoomGroupingMethod.ByLegacyGroup
                            && nonNullViewState.selectedGroupingMethod.group()?.groupId == groupSummary.groupId)
                    listener { callback?.onGroupSelected(groupSummary) }
                }
            }
        }
    }

    private fun buildGroupModels(summaries: List<RoomSummary>?,
                                 selected: RoomGroupingMethod,
                                 rootSpaces: List<RoomSummary>?,
                                 expandedStates: Map<String, Boolean>,
                                 homeCount: RoomAggregateNotificationCount) {
        genericItemHeader {
            id("spaces")
            text(stringProvider.getString(R.string.spaces_header))
        }

        spaceBetaHeaderItem { id("beta_header") }

        // show invites on top

        summaries?.filter { it.membership == Membership.INVITE }
                ?.forEach {
                    spaceSummaryItem {
                        avatarRenderer(avatarRenderer)
                        id(it.roomId)
                        matrixItem(it.toMatrixItem())
                        countState(UnreadCounterBadgeView.State(1, true))
                        selected(false)
                        description(stringProvider.getString(R.string.you_are_invited))
                        listener { callback?.onSpaceInviteSelected(it) }
                    }
                }

        homeSpaceSummaryItem {
            id("space_home")
            selected(selected is RoomGroupingMethod.BySpace && selected.space() == null)
            countState(UnreadCounterBadgeView.State(homeCount.totalCount, homeCount.isHighlight))
            listener { callback?.onSpaceSelected(null) }
        }

        rootSpaces
                ?.sortedBy { it.displayName }
                ?.forEach { groupSummary ->
                    val isSelected = selected is RoomGroupingMethod.BySpace && groupSummary.roomId == selected.space()?.roomId
                    // does it have children?
                    val subSpaces = groupSummary.spaceChildren?.filter { childInfo ->
                        summaries?.indexOfFirst { it.roomId == childInfo.childRoomId } != -1
                    }
                    val hasChildren = (subSpaces?.size ?: 0) > 0
                    val expanded = expandedStates[groupSummary.roomId] == true

                    spaceSummaryItem {
                        avatarRenderer(avatarRenderer)
                        id(groupSummary.roomId)
                        hasChildren(hasChildren)
                        expanded(expanded)
                        matrixItem(groupSummary.toMatrixItem())
                        selected(isSelected)
                        onMore { callback?.onSpaceSettings(groupSummary) }
                        listener { callback?.onSpaceSelected(groupSummary) }
                        toggleExpand { callback?.onToggleExpand(groupSummary) }
                        countState(
                                UnreadCounterBadgeView.State(
                                        groupSummary.notificationCount,
                                        groupSummary.highlightCount > 0
                                )
                        )
                    }

                    if (hasChildren && expanded) {
                        // it's expanded
                        subSpaces?.forEach { child ->
                            summaries?.firstOrNull { it.roomId == child.childRoomId }?.let { childSum ->
                                val isChildSelected = selected is RoomGroupingMethod.BySpace && childSum.roomId == selected.space()?.roomId
                                spaceSummaryItem {
                                    avatarRenderer(avatarRenderer)
                                    id(child.childRoomId)
                                    hasChildren(false)
                                    selected(isChildSelected)
                                    matrixItem(MatrixItem.RoomItem(child.childRoomId, child.name, child.avatarUrl))
                                    listener { callback?.onSpaceSelected(childSum) }
                                    indent(1)
                                    countState(
                                            UnreadCounterBadgeView.State(
                                                    groupSummary.notificationCount,
                                                    groupSummary.highlightCount > 0
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

        spaceAddItem {
            id("create")
            listener { callback?.onAddSpaceSelected() }
        }
    }

    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary?)
        fun onSpaceInviteSelected(spaceSummary: RoomSummary)
        fun onSpaceSettings(spaceSummary: RoomSummary)
        fun onToggleExpand(spaceSummary: RoomSummary)
        fun onAddSpaceSelected()

        fun onGroupSelected(groupSummary: GroupSummary?)
    }
}
