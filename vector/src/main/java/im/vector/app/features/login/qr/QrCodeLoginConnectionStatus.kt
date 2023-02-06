/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.login.qr

import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason

sealed class QrCodeLoginConnectionStatus {
    object ConnectingToDevice : QrCodeLoginConnectionStatus()
    data class Connected(val securityCode: String, val canConfirmSecurityCode: Boolean) : QrCodeLoginConnectionStatus()
    object SigningIn : QrCodeLoginConnectionStatus()
    data class Failed(val errorType: RendezvousFailureReason, val canTryAgain: Boolean) : QrCodeLoginConnectionStatus()
}
