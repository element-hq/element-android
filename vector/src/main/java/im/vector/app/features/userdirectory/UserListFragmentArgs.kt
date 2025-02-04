/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.userdirectory

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserListFragmentArgs(
        val title: String,
        val menuResId: Int,
        val submitMenuItemId: Int,
        val excludedUserIds: Set<String>? = null,
        val singleSelection: Boolean = false,
        val single3pidSelection: Boolean = false,
        val showInviteActions: Boolean = true,
        val showContactBookAction: Boolean = true,
        val showToolbar: Boolean = true
) : Parcelable
