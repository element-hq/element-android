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
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericHeaderItem
import im.vector.app.features.grouplist.groupSummaryItem
import im.vector.app.features.grouplist.homeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.group
import im.vector.app.space
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceModalController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
//        private val colorProvider: ColorProvider,
//        private val stringProvider: StringProvider
) : EpoxyController() {

    var callback: Callback? = null
    private var viewState: SpaceListViewState? = null

    fun update(viewState: SpaceListViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        viewState?.apply {
            buildGroupModels(selectedGroupingMethod, rootSpacesOrdered, expandedStates)
        }
    }

    private fun buildGroupModels(selected: RoomGroupingMethod,
                                 rootSpaces: List<RoomSummary>?,
                                 expandedStates: Map<String, Boolean>) {
        val host = this

        rootSpaces?.forEach { spaceSummary ->
            val isSelected = selected is RoomGroupingMethod.BySpace && spaceSummary.roomId == selected.space()?.roomId
            val expanded = expandedStates[spaceSummary.roomId] == true

            spaceSummaryItem {
                avatarRenderer(host.avatarRenderer)
                id(spaceSummary.roomId)
                expanded(expanded)
                matrixItem(spaceSummary.toMatrixItem())
                selected(isSelected)
                canDrag(true)
                listener { host.callback?.onSpaceSelected(spaceSummary) }
                countState(
                        UnreadCounterBadgeView.State(
                                spaceSummary.notificationCount,
                                spaceSummary.highlightCount > 0
                        )
                )
            }
        }
    }

    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary?)
    }
}
