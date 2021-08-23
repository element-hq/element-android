/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.threepids

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.identity.ThreePid

sealed class ThreePidsSettingsAction : VectorViewModelAction {
    data class ChangeUiState(val newUiState: ThreePidsSettingsUiState) : ThreePidsSettingsAction()
    data class AddThreePid(val threePid: ThreePid) : ThreePidsSettingsAction()
    data class SubmitCode(val threePid: ThreePid.Msisdn, val code: String) : ThreePidsSettingsAction()
    data class ContinueThreePid(val threePid: ThreePid) : ThreePidsSettingsAction()
    data class CancelThreePid(val threePid: ThreePid) : ThreePidsSettingsAction()

    //    data class AccountPassword(val password: String) : ThreePidsSettingsAction()
    data class DeleteThreePid(val threePid: ThreePid) : ThreePidsSettingsAction()

    object SsoAuthDone : ThreePidsSettingsAction()
    data class PasswordAuthDone(val password: String) : ThreePidsSettingsAction()
    object ReAuthCancelled : ThreePidsSettingsAction()
}
