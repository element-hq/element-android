/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles

import androidx.annotation.DrawableRes
import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

fun EpoxyController.buildProfileSection(title: String) {
    profileSectionItem {
        id("section_$title")
        title(title)
    }
}

fun EpoxyController.buildProfileAction(
        id: String,
        title: String,
        subtitle: String? = null,
        editable: Boolean = true,
        @DrawableRes icon: Int = 0,
        tintIcon: Boolean = true,
        @DrawableRes editableRes: Int? = null,
        destructive: Boolean = false,
        divider: Boolean = true,
        action: ClickListener? = null,
        @DrawableRes accessory: Int = 0,
        accessoryMatrixItem: MatrixItem? = null,
        avatarRenderer: AvatarRenderer? = null
) {
    profileActionItem {
        iconRes(icon)
        tintIcon(tintIcon)
        id("action_$id")
        subtitle(subtitle)
        editable(editable)
        editableRes?.let { editableRes(editableRes) }
        destructive(destructive)
        title(title)
        accessoryRes(accessory)
        accessoryMatrixItem(accessoryMatrixItem)
        avatarRenderer(avatarRenderer)
        listener(action)
    }

    if (divider) {
        dividerItem {
            id("divider_$title")
        }
    }
}
