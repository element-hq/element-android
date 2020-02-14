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

package im.vector.riotx.features.crypto.quads

import im.vector.riotx.core.platform.VectorViewEvents
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.core.platform.WaitingViewData

sealed class SharedSecureStorageAction : VectorViewModelAction {

    object TogglePasswordVisibility : SharedSecureStorageAction()
    object Cancel : SharedSecureStorageAction()
    data class SubmitPassphrase(val passphrase: String) : SharedSecureStorageAction()
}

sealed class SharedSecureStorageViewEvent : VectorViewEvents {

    object Dismiss : SharedSecureStorageViewEvent()
    data class Error(val message: String) : SharedSecureStorageViewEvent()
    data class InlineError(val message: String) : SharedSecureStorageViewEvent()
    object ShowModalLoading : SharedSecureStorageViewEvent()
    object HideModalLoading : SharedSecureStorageViewEvent()
    data class UpdateLoadingState(val waitingData: WaitingViewData) : SharedSecureStorageViewEvent()
}
