/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.alias.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.core.platform.VectorSharedAction

sealed class RoomAliasBottomSheetSharedAction(
        @StringRes val titleRes: Int,
        @DrawableRes val iconResId: Int = 0,
        val destructive: Boolean = false
) :
        VectorSharedAction {

    data class ShareAlias(val matrixTo: String) : RoomAliasBottomSheetSharedAction(
            R.string.action_share,
            R.drawable.ic_material_share
    )

    data class PublishAlias(val alias: String) : RoomAliasBottomSheetSharedAction(
            R.string.room_alias_action_publish
    )

    data class UnPublishAlias(val alias: String) : RoomAliasBottomSheetSharedAction(
            R.string.room_alias_action_unpublish
    )

    data class DeleteAlias(val alias: String) : RoomAliasBottomSheetSharedAction(
            R.string.action_delete,
            R.drawable.ic_trash_24,
            true
    )

    data class SetMainAlias(val alias: String) : RoomAliasBottomSheetSharedAction(
            R.string.room_settings_set_main_address
    )

    object UnsetMainAlias : RoomAliasBottomSheetSharedAction(
            R.string.room_settings_unset_main_address
    )
}
