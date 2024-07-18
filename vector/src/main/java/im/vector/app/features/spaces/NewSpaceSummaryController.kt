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
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.grouplist.newHomeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.session.user.model.User
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
        buildGroupModels(nonNullViewState)
    }

    private fun buildGroupModels(viewState: SpaceListViewState) = with(viewState) {
        addHeaderItem()
        addHomeItem(selectedSpace == null, homeAggregateCount)
        addSpaces(spaces, selectedSpace, rootSpacesOrdered, expandedStates)
        addInvites(selectedSpace, rootSpacesOrdered, inviters)
        addCreateItem()
    }

    private fun addHeaderItem() {
        newSpaceListHeaderItem {
            id("space_list_header")
        }
    }

    private fun addHomeItem(selected: Boolean, homeCount: RoomAggregateNotificationCount) {
        val host = this
        newHomeSpaceSummaryItem {
            id("space_home")
            text(host.stringProvider.getString(CommonStrings.all_chats))
            selected(selected)
            countState(UnreadCounterBadgeView.State.Count(homeCount.totalCount, homeCount.isHighlight))
            listener { host.callback?.onSpaceSelected(null, isSubSpace = false) }
        }
    }

    private fun addSpaces(
            spaceSummaries: List<RoomSummary>?,
            selectedSpace: RoomSummary?,
            rootSpaces: List<RoomSummary>?,
            expandedStates: Map<String, Boolean>,
    ) {
        val host = this

        rootSpaces?.filterNot { it.membership == Membership.INVITE }
                ?.forEach { spaceSummary ->
                    val subSpaces = spaceSummary.spaceChildren?.filter { spaceChild -> spaceSummaries.containsSpaceId(spaceChild.childRoomId) }
                    val hasChildren = (subSpaces?.size ?: 0) > 0
                    val isSelected = spaceSummary.roomId == selectedSpace?.roomId
                    val expanded = expandedStates[spaceSummary.roomId] == true

                    newSpaceSummaryItem {
                        id(spaceSummary.roomId)
                        avatarRenderer(host.avatarRenderer)
                        countState(UnreadCounterBadgeView.State.Count(spaceSummary.notificationCount, spaceSummary.highlightCount > 0))
                        expanded(expanded)
                        hasChildren(hasChildren)
                        matrixItem(spaceSummary.toMatrixItem())
                        onLongClickListener { host.callback?.onSpaceSettings(spaceSummary) }
                        onSpaceSelectedListener { host.callback?.onSpaceSelected(spaceSummary, isSubSpace = false) }
                        onToggleExpandListener { host.callback?.onToggleExpand(spaceSummary) }
                        selected(isSelected)
                    }

                    if (hasChildren && expanded) {
                        subSpaces?.forEach { child ->
                            addSubSpace(spaceSummary.roomId, spaceSummaries, expandedStates, selectedSpace, child, 1)
                        }
                    }
                }
    }

    private fun List<RoomSummary>?.containsSpaceId(spaceId: String) = this?.any { it.roomId == spaceId }.orFalse()

    private fun addSubSpace(
            idPrefix: String,
            spaceSummaries: List<RoomSummary>?,
            expandedStates: Map<String, Boolean>,
            selectedSpace: RoomSummary?,
            info: SpaceChildInfo,
            depth: Int,
    ) {
        val host = this
        val childSummary = spaceSummaries?.firstOrNull { it.roomId == info.childRoomId } ?: return
        val id = "$idPrefix:${childSummary.roomId}"
        val countState = UnreadCounterBadgeView.State.Count(childSummary.notificationCount, childSummary.highlightCount > 0)
        val expanded = expandedStates[childSummary.roomId] == true
        val isSelected = childSummary.roomId == selectedSpace?.roomId
        val subSpaces = childSummary.spaceChildren?.filter { childSpace -> spaceSummaries.containsSpaceId(childSpace.childRoomId) }
                ?.sortedWith(subSpaceComparator)

        newSubSpaceSummaryItem {
            id(id)
            avatarRenderer(host.avatarRenderer)
            countState(countState)
            expanded(expanded)
            hasChildren(!subSpaces.isNullOrEmpty())
            indent(depth)
            matrixItem(childSummary.toMatrixItem())
            onLongClickListener { host.callback?.onSpaceSettings(childSummary) }
            onSubSpaceSelectedListener { host.callback?.onSpaceSelected(childSummary, isSubSpace = true) }
            onToggleExpandListener { host.callback?.onToggleExpand(childSummary) }
            selected(isSelected)
        }

        if (expanded) {
            subSpaces?.forEach {
                addSubSpace(id, spaceSummaries, expandedStates, selectedSpace, it, depth + 1)
            }
        }
    }

    private fun addInvites(
            selectedSpace: RoomSummary?,
            rootSpaces: List<RoomSummary>?,
            inviters: List<User>,
    ) {
        val host = this

        rootSpaces?.filter { it.membership == Membership.INVITE }
                ?.forEach { spaceSummary ->
                    val isSelected = spaceSummary.roomId == selectedSpace?.roomId
                    val inviter = inviters.find { it.userId == spaceSummary.inviterId }

                    spaceInviteItem {
                        id(spaceSummary.roomId)
                        avatarRenderer(host.avatarRenderer)
                        inviter(inviter?.displayName.orEmpty())
                        matrixItem(spaceSummary.toMatrixItem())
                        onLongClickListener { host.callback?.onSpaceSettings(spaceSummary) }
                        onInviteSelectedListener { host.callback?.onSpaceInviteSelected(spaceSummary) }
                        selected(isSelected)
                    }
                }
    }

    private fun addCreateItem() {
        val host = this
        newSpaceAddItem {
            id("create")
            listener { host.callback?.onAddSpaceSelected() }
        }
    }

    /**
     * This is a full duplicate of [SpaceSummaryController.Callback]. We need to merge them ASAP*/
    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary?, isSubSpace: Boolean)
        fun onSpaceInviteSelected(spaceSummary: RoomSummary)
        fun onSpaceSettings(spaceSummary: RoomSummary)
        fun onToggleExpand(spaceSummary: RoomSummary)
        fun onAddSpaceSelected()
        fun sendFeedBack()
    }
}
