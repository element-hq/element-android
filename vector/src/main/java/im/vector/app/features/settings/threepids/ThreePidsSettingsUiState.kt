/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.threepids

sealed class ThreePidsSettingsUiState {
    object Idle : ThreePidsSettingsUiState()
    data class AddingEmail(val error: String?) : ThreePidsSettingsUiState()
    data class AddingPhoneNumber(val error: String?) : ThreePidsSettingsUiState()
}
