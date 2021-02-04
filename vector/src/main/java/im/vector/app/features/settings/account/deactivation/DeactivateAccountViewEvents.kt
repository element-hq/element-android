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

package im.vector.app.features.settings.account.deactivation

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse

/**
 * Transient events for deactivate account settings screen
 */
sealed class DeactivateAccountViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : DeactivateAccountViewEvents()
    object InvalidAuth : DeactivateAccountViewEvents()
    data class OtherFailure(val throwable: Throwable) : DeactivateAccountViewEvents()
    object Done : DeactivateAccountViewEvents()
    data class RequestReAuth(val registrationFlowResponse: RegistrationFlowResponse, val lastErrorCode: String?) : DeactivateAccountViewEvents()
}
