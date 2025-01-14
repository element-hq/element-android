/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.threepids

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.utils.ReadOnceTrue
import org.matrix.android.sdk.api.session.identity.ThreePid

data class ThreePidsSettingsViewState(
        val uiState: ThreePidsSettingsUiState = ThreePidsSettingsUiState.Idle,
        val isLoading: Boolean = false,
        val threePids: Async<List<ThreePid>> = Uninitialized,
        val pendingThreePids: Async<List<ThreePid>> = Uninitialized,
        val msisdnValidationRequests: Map<String, Async<Unit>> = emptyMap(),
        val editTextReinitiator: ReadOnceTrue = ReadOnceTrue(),
        val msisdnValidationReinitiator: Map<ThreePid, ReadOnceTrue> = emptyMap()
) : MavericksState
