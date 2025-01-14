/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.ignored

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.user.model.User

data class IgnoredUsersViewState(
        val ignoredUsers: List<User> = emptyList(),
        val isLoading: Boolean = false
) : MavericksState
