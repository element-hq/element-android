/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import javax.inject.Inject

class RoleFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(role: Role): String {
        return when (role) {
            Role.Admin -> stringProvider.getString(R.string.power_level_admin)
            Role.Moderator -> stringProvider.getString(R.string.power_level_moderator)
            Role.Default -> stringProvider.getString(R.string.power_level_default)
            is Role.Custom -> stringProvider.getString(R.string.power_level_custom, role.value)
        }
    }
}
