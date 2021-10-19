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

package fr.gouv.tchap.features.roomprofile.settings.linkaccess.detail

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.app.core.ui.bottomsheet.bottomSheetTitleItem
import javax.inject.Inject

/**
 * Epoxy controller for room link access actions
 */
class TchapRoomLinkAccessBottomSheetController @Inject constructor() : TypedEpoxyController<TchapRoomLinkAccessBottomSheetState>() {

    var listener: Listener? = null

    override fun buildModels(state: TchapRoomLinkAccessBottomSheetState) {
        bottomSheetTitleItem {
            id("alias")
            title(state.alias)
            subTitle(state.matrixToLink)
        }

        bottomSheetDividerItem {
            id("aliasSeparator")
        }

        var idx = 0
        state.matrixToLink?.let {
            // Copy
            TchapRoomLinkAccessBottomSheetSharedAction.CopyLink(it).toBottomSheetItem(++idx)
            // Forward
            TchapRoomLinkAccessBottomSheetSharedAction.ForwardLink(it).toBottomSheetItem(++idx)
            // Share
            TchapRoomLinkAccessBottomSheetSharedAction.ShareLink(it).toBottomSheetItem(++idx)
        }
    }

    private fun TchapRoomLinkAccessBottomSheetSharedAction.toBottomSheetItem(index: Int) {
        val host = this@TchapRoomLinkAccessBottomSheetController
        return bottomSheetActionItem {
            id("action_$index")
            iconRes(iconResId)
            textRes(titleRes)
            listener { host.listener?.didSelectMenuAction(this@toBottomSheetItem) }
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: TchapRoomLinkAccessBottomSheetSharedAction)
    }
}
