/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.signout.soft

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.login.LoginMode

data class SoftLogoutViewState(
        val asyncHomeServerLoginFlowRequest: Async<LoginMode> = Uninitialized,
        val asyncLoginAction: Async<Unit> = Uninitialized,
        val homeServerUrl: String,
        val userId: String,
        val deviceId: String,
        val userDisplayName: String,
        val hasUnsavedKeys: Boolean,
        val enteredPassword: String = ""
) : MavericksState {

    fun isLoading(): Boolean {
        return asyncLoginAction is Loading ||
                // Keep loading when it is success because of the delay to switch to the next Activity
                asyncLoginAction is Success
    }
}
