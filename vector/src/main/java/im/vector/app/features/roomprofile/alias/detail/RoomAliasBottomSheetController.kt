/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.roomprofile.alias.detail

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.app.core.ui.bottomsheet.bottomSheetTitleItem
import javax.inject.Inject

/**
 * Epoxy controller for room alias actions.
 */
class RoomAliasBottomSheetController @Inject constructor() : TypedEpoxyController<RoomAliasBottomSheetState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomAliasBottomSheetState) {
        bottomSheetTitleItem {
            id("alias")
            title(state.alias)
            subTitle(state.matrixToLink)
        }

        // Notifications
        bottomSheetDividerItem {
            id("aliasSeparator")
        }

        var idx = 0
        // Share
        state.matrixToLink?.let {
            RoomAliasBottomSheetSharedAction.ShareAlias(it).toBottomSheetItem(++idx)
        }

        // Action on published alias
        if (state.isPublished) {
            // Published address
            if (state.canEditCanonicalAlias) {
                if (state.isMainAlias) {
                    RoomAliasBottomSheetSharedAction.UnsetMainAlias.toBottomSheetItem(++idx)
                } else {
                    RoomAliasBottomSheetSharedAction.SetMainAlias(state.alias).toBottomSheetItem(++idx)
                }
                RoomAliasBottomSheetSharedAction.UnPublishAlias(state.alias).toBottomSheetItem(++idx)
            }
        }

        if (state.isLocal) {
            // Local address
            if (state.canEditCanonicalAlias && state.isPublished.not()) {
                // Publish
                RoomAliasBottomSheetSharedAction.PublishAlias(state.alias).toBottomSheetItem(++idx)
            }
            // Delete
            RoomAliasBottomSheetSharedAction.DeleteAlias(state.alias).toBottomSheetItem(++idx)
        }
    }

    private fun RoomAliasBottomSheetSharedAction.toBottomSheetItem(index: Int) {
        val host = this@RoomAliasBottomSheetController
        return bottomSheetActionItem {
            id("action_$index")
            iconRes(iconResId)
            textRes(titleRes)
            destructive(this@toBottomSheetItem.destructive)
            listener { host.listener?.didSelectMenuAction(this@toBottomSheetItem) }
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickAction: RoomAliasBottomSheetSharedAction)
    }
}
