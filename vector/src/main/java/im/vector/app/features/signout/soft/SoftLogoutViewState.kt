/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.signout.soft

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.login.LoginMode
import org.matrix.android.sdk.api.auth.LoginType

data class SoftLogoutViewState(
        val asyncHomeServerLoginFlowRequest: Async<LoginMode> = Uninitialized,
        val asyncLoginAction: Async<Unit> = Uninitialized,
        val homeServerUrl: String,
        val userId: String,
        val deviceId: String,
        val userDisplayName: String,
        val hasUnsavedKeys: Async<Boolean> = Uninitialized,
        val loginType: LoginType,
        val enteredPassword: String = "",
) : MavericksState {

    val isLoading: Boolean =
            asyncLoginAction is Loading ||
                // Keep loading when it is success because of the delay to switch to the next Activity
                asyncLoginAction is Success
}
