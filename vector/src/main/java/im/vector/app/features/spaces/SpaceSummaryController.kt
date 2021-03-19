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
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItemHeader
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.grouplist.homeSpaceSummaryItem
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
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
        buildGroupModels(nonNullViewState.asyncSpaces(), nonNullViewState.selectedSpace)
    }

    private fun buildGroupModels(summaries: List<RoomSummary>?, selected: RoomSummary?) {
        if (summaries.isNullOrEmpty()) {
            return
        }
        // show invites on top

        summaries.filter { it.membership == Membership.INVITE }
                .let { invites ->
                    if (invites.isNotEmpty()) {
                        genericItemHeader {
                            id("invites")
                            text(stringProvider.getString(R.string.spaces_invited_header))
                        }
                        invites.forEach {
                            spaceSummaryItem {
                                avatarRenderer(avatarRenderer)
                                id(it.roomId)
                                matrixItem(it.toMatrixItem())
                                selected(false)
                                listener { callback?.onSpaceSelected(it) }
//                                lea { callback?.onSpaceSelected(it) }
                            }
                        }
                        genericFooterItem {
                            id("invite_space")
                            text("")
                        }
                    }
                }

        genericItemHeader {
            id("spaces")
            text(stringProvider.getString(R.string.spaces_header))
        }

        summaries
                .filter { it.membership == Membership.JOIN }
                .forEach { groupSummary ->
                    val isSelected = groupSummary.roomId == selected?.roomId
                    if (groupSummary.roomId == ALL_COMMUNITIES_GROUP_ID) {
                        homeSpaceSummaryItem {
                            id(groupSummary.roomId)
                            selected(isSelected)
                            listener { callback?.onSpaceSelected(groupSummary) }
                        }
                    } else {
                        spaceSummaryItem {
                            avatarRenderer(avatarRenderer)
                            id(groupSummary.roomId)
                            matrixItem(groupSummary.toMatrixItem())
                            selected(isSelected)
                            onMore { callback?.onSpaceSettings(groupSummary) }
                            listener { callback?.onSpaceSelected(groupSummary) }
                        }
                    }
                }

        // Temporary item to create a new Space (will move with final design)

        genericButtonItem {
            id("create")
            text(stringProvider.getString(R.string.add_space))
            iconRes(R.drawable.ic_add_black)
            buttonClickAction(DebouncedClickListener({ callback?.onAddSpaceSelected() }))
        }
    }

    interface Callback {
        fun onSpaceSelected(spaceSummary: RoomSummary)
        fun onSpaceSettings(spaceSummary: RoomSummary)
        fun onAddSpaceSelected()
    }
}
