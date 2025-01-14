/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.grouplist.homeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceSummaryController @Inject constructor(
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
                nonNullViewState.asyncSpaces(),
                nonNullViewState.selectedSpace,
                nonNullViewState.rootSpacesOrdered,
                nonNullViewState.expandedStates,
                nonNullViewState.homeAggregateCount
        )
    }

    private fun buildGroupModels(
            summaries: List<RoomSummary>?,
            selectedSpace: RoomSummary?,
            rootSpaces: List<RoomSummary>?,
            expandedStates: Map<String, Boolean>,
            homeCount: RoomAggregateNotificationCount
    ) {
        val host = this
        spaceBetaHeaderItem {
            id("beta_header")
        }

        // show invites on top
        summaries
                ?.filter { it.membership == Membership.INVITE }
                ?.forEach { roomSummary ->
                    spaceSummaryItem {
                        avatarRenderer(host.avatarRenderer)
                        id("invite_${roomSummary.roomId}")
                        matrixItem(roomSummary.toMatrixItem())
                        countState(UnreadCounterBadgeView.State.Count(1, true))
                        selected(false)
                        description(host.stringProvider.getString(CommonStrings.you_are_invited))
                        canDrag(false)
                        listener { host.callback?.onSpaceInviteSelected(roomSummary) }
                    }
                }

        homeSpaceSummaryItem {
            id("space_home")
            selected(selectedSpace == null)
            countState(UnreadCounterBadgeView.State.Count(homeCount.totalCount, homeCount.isHighlight))
            listener { host.callback?.onSpaceSelected(null, isSubSpace = false) }
        }

        rootSpaces
                ?.filter { it.membership != Membership.INVITE }
                ?.forEach { roomSummary ->
                    val isSelected = roomSummary.roomId == selectedSpace?.roomId
                    // does it have children?
                    val subSpaces = roomSummary.spaceChildren?.filter { childInfo ->
                        summaries?.any { it.roomId == childInfo.childRoomId }.orFalse()
                    }?.sortedWith(subSpaceComparator)
                    val hasChildren = (subSpaces?.size ?: 0) > 0
                    val expanded = expandedStates[roomSummary.roomId] == true

                    spaceSummaryItem {
                        avatarRenderer(host.avatarRenderer)
                        id(roomSummary.roomId)
                        hasChildren(hasChildren)
                        expanded(expanded)
                        // to debug order
                        // matrixItem(groupSummary.copy(displayName = "${groupSummary.displayName} / ${spaceOrderInfo?.get(groupSummary.roomId)}")
                        // .toMatrixItem())
                        matrixItem(roomSummary.toMatrixItem())
                        selected(isSelected)
                        canDrag(true)
                        onMore { host.callback?.onSpaceSettings(roomSummary) }
                        listener { host.callback?.onSpaceSelected(roomSummary, isSubSpace = false) }
                        toggleExpand { host.callback?.onToggleExpand(roomSummary) }
                        countState(
                                UnreadCounterBadgeView.State.Count(
                                        roomSummary.notificationCount,
                                        roomSummary.highlightCount > 0
                                )
                        )
                    }

                    if (hasChildren && expanded) {
                        // it's expanded
                        subSpaces?.forEach { child ->
                            buildSubSpace(roomSummary.roomId, summaries, expandedStates, selectedSpace, child, 1, 3)
                        }
                    }
                }

        spaceAddItem {
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

        subSpaceSummaryItem {
            avatarRenderer(host.avatarRenderer)
            id(id)
            hasChildren(!subSpaces.isNullOrEmpty())
            selected(isSelected)
            expanded(expanded)
            onMore { host.callback?.onSpaceSettings(childSummary) }
            matrixItem(childSummary.toMatrixItem())
            listener { host.callback?.onSpaceSelected(childSummary, isSubSpace = true) }
            toggleExpand { host.callback?.onToggleExpand(childSummary) }
            indent(currentDepth)
            countState(
                    UnreadCounterBadgeView.State.Count(
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

    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary?, isSubSpace: Boolean)
        fun onSpaceInviteSelected(spaceSummary: RoomSummary)
        fun onSpaceSettings(spaceSummary: RoomSummary)
        fun onToggleExpand(spaceSummary: RoomSummary)
        fun onAddSpaceSelected()
        fun sendFeedBack()
    }
}
