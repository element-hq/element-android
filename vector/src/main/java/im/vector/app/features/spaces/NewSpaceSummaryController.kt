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
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.grouplist.newHomeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class NewSpaceSummaryController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
) : EpoxyController() {

    var callback: Callback? = null
    private var viewState: SpaceListViewState? = null

    private val subSpaceComparator: Comparator<SpaceChildInfo> = compareBy<SpaceChildInfo> { it.order }.thenBy { it.childRoomId }

    fun update(viewState: SpaceListViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val nonNullViewState = viewState ?: return
        buildGroupModels(
                nonNullViewState.spaces,
                nonNullViewState.selectedSpace,
                nonNullViewState.rootSpacesOrdered,
                nonNullViewState.homeAggregateCount,
                nonNullViewState.expandedStates,
        )
    }

    private fun buildGroupModels(
            spaceSummaries: List<RoomSummary>?,
            selectedSpace: RoomSummary?,
            rootSpaces: List<RoomSummary>?,
            homeCount: RoomAggregateNotificationCount,
            expandedStates: Map<String, Boolean>,
    ) {
        println(homeCount)
        val host = this
        newSpaceListHeaderItem {
            id("space_list_header")
        }

        addHomeItem(false, homeCount)

        rootSpaces
                ?.filter { it.membership != Membership.INVITE }
                ?.forEach { spaceSummary ->

                    val subSpaces = spaceSummary.spaceChildren?.filter { childInfo ->
                        spaceSummaries?.any { it.roomId == childInfo.childRoomId }.orFalse()
                    }
                    val hasChildren = (subSpaces?.size ?: 0) > 0
                    val isSelected = spaceSummary.roomId == selectedSpace?.roomId
                    val expanded = expandedStates[spaceSummary.roomId] == true

                    newSpaceSummaryItem {
                        avatarRenderer(host.avatarRenderer)
                        id(spaceSummary.roomId)
                        matrixItem(spaceSummary.toMatrixItem())
                        onSpaceSelectedListener { host.callback?.onSpaceSelected(spaceSummary) }
                        countState(UnreadCounterBadgeView.State(spaceSummary.notificationCount, spaceSummary.highlightCount > 0))

                        expanded(expanded)
                        hasChildren(hasChildren)
                        toggleExpand { host.callback?.onToggleExpand(spaceSummary) }
                        selected(isSelected)
                        onMore { host.callback?.onSpaceSettings(spaceSummary) }
                    }

                    if (hasChildren && expanded) {
                        // it's expanded
                        subSpaces?.forEach { child ->
                            buildSubSpace(spaceSummary.roomId, spaceSummaries, expandedStates, selectedSpace, child, 1, 3)
                        }
                    }
                }

        newSpaceAddItem {
            id("create")
            listener { host.callback?.onAddSpaceSelected() }
        }
    }

    private fun buildSubSpace(
            idPrefix: String,
            summaries: List<RoomSummary>?,
            expandedStates: Map<String, Boolean>,
            selectedSpace: RoomSummary?,
            info: SpaceChildInfo, currentDepth: Int, maxDepth: Int
    ) {
        val host = this
        if (currentDepth >= maxDepth) return
        val childSummary = summaries?.firstOrNull { it.roomId == info.childRoomId } ?: return
        // does it have children?
        val subSpaces = childSummary.spaceChildren?.filter { childInfo ->
            summaries.any { it.roomId == childInfo.childRoomId }
        }?.sortedWith(subSpaceComparator)
        val expanded = expandedStates[childSummary.roomId] == true
        val isSelected = childSummary.roomId == selectedSpace?.roomId

        val id = "$idPrefix:${childSummary.roomId}"

        newSubSpaceSummaryItem {
            avatarRenderer(host.avatarRenderer)
            id(id)
            hasChildren(!subSpaces.isNullOrEmpty())
            selected(isSelected)
            expanded(expanded)
            onMore { host.callback?.onSpaceSettings(childSummary) }
            matrixItem(childSummary.toMatrixItem())
            listener { host.callback?.onSpaceSelected(childSummary) }
            toggleExpand { host.callback?.onToggleExpand(childSummary) }
            indent(currentDepth)
            countState(
                    UnreadCounterBadgeView.State(
                            childSummary.notificationCount,
                            childSummary.highlightCount > 0
                    )
            )
        }

        if (expanded) {
            subSpaces?.forEach {
                buildSubSpace(id, summaries, expandedStates, selectedSpace, it, currentDepth + 1, maxDepth)
            }
        }
    }

    private fun addHomeItem(selected: Boolean, homeCount: RoomAggregateNotificationCount) {
        val host = this
        newHomeSpaceSummaryItem {
            id("space_home")
            text(host.stringProvider.getString(R.string.all_chats))
            selected(selected)
            countState(UnreadCounterBadgeView.State(homeCount.totalCount, homeCount.isHighlight))
            listener { host.callback?.onSpaceSelected(null) }
        }
    }

    private fun addSubSpaces(
            selectedSpace: RoomSummary,
            spaceSummaries: List<RoomSummary>?,
            homeCount: RoomAggregateNotificationCount,
    ) {
        val host = this
        val spaceChildren = selectedSpace.spaceChildren
        var subSpacesAdded = false

        spaceChildren?.sortedWith(subSpaceComparator)?.forEach { spaceChild ->
            val subSpaceSummary = spaceSummaries?.firstOrNull { it.roomId == spaceChild.childRoomId } ?: return@forEach

            if (subSpaceSummary.membership != Membership.INVITE) {
                subSpacesAdded = true
                newSpaceSummaryItem {
                    avatarRenderer(host.avatarRenderer)
                    id(subSpaceSummary.roomId)
                    matrixItem(subSpaceSummary.toMatrixItem())
                    selected(false)
                    onSpaceSelectedListener { host.callback?.onSpaceSelected(subSpaceSummary) }
                    countState(
                            UnreadCounterBadgeView.State(
                                    subSpaceSummary.notificationCount,
                                    subSpaceSummary.highlightCount > 0
                            )
                    )
                }
            }
        }

        if (!subSpacesAdded) {
            addHomeItem(false, homeCount)
        }
    }

    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary?)
        fun onSpaceInviteSelected(spaceSummary: RoomSummary)
        fun onSpaceSettings(spaceSummary: RoomSummary)
        fun onToggleExpand(spaceSummary: RoomSummary)
        fun onAddSpaceSelected()
        fun sendFeedBack()
    }
}
