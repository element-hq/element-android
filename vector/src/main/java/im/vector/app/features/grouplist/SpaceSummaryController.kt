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
 *
 */

package im.vector.app.features.grouplist

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.spaces.SpaceListViewState
import org.matrix.android.sdk.api.session.space.SpaceSummary
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

    private fun buildGroupModels(summaries: List<SpaceSummary>?, selected: SpaceSummary?) {
        if (summaries.isNullOrEmpty()) {
            return
        }
        summaries.forEach { groupSummary ->
            val isSelected = groupSummary.spaceId == selected?.spaceId
            if (groupSummary.spaceId == ALL_COMMUNITIES_GROUP_ID) {
                homeSpaceSummaryItem {
                    id(groupSummary.spaceId)
                    selected(isSelected)
                    listener { callback?.onSpaceSelected(groupSummary) }
                }
            } else {
                spaceSummaryItem {
                    avatarRenderer(avatarRenderer)
                    id(groupSummary.spaceId)
                    matrixItem(groupSummary.toMatrixItem())
                    selected(isSelected)
                    listener { callback?.onSpaceSelected(groupSummary) }
                }
            }
        }
    }

    interface Callback {
        fun onSpaceSelected(spaceSummary: SpaceSummary)
    }
}
