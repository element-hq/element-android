/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.onboarding.AuthenticationDescription
import org.matrix.android.sdk.api.session.sync.SyncRequestState

data class HomeActivityViewState(
        val syncRequestState: SyncRequestState = SyncRequestState.Idle,
        val authenticationDescription: AuthenticationDescription? = null,
) : MavericksState
