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

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import timber.log.Timber

class QrCodeLoginViewModel @AssistedInject constructor(
        @Assisted private val initialState: QrCodeLoginViewState,
) : VectorViewModel<QrCodeLoginViewState, QrCodeLoginAction, QrCodeLoginViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<QrCodeLoginViewModel, QrCodeLoginViewState> {
        override fun create(initialState: QrCodeLoginViewState): QrCodeLoginViewModel
    }

    companion object : MavericksViewModelFactory<QrCodeLoginViewModel, QrCodeLoginViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: QrCodeLoginAction) {
        when (action) {
            is QrCodeLoginAction.OnQrCodeScanned -> handleOnQrCodeScanned(action)
            QrCodeLoginAction.GenerateQrCode -> handleQrCodeViewStarted()
            QrCodeLoginAction.ShowQrCode -> handleShowQrCode()
        }
    }

    private fun handleShowQrCode() {
        _viewEvents.post(QrCodeLoginViewEvents.NavigateToShowQrCodeScreen)
    }

    private fun handleQrCodeViewStarted() {
        val qrCodeData = generateQrCodeData()
        setState {
            copy(
                    generatedQrCodeData = qrCodeData
            )
        }
    }

    private fun handleOnQrCodeScanned(action: QrCodeLoginAction.OnQrCodeScanned) {
        if (isValidQrCode(action.qrCode)) {
            setState {
                copy(
                        connectionStatus = QrCodeLoginConnectionStatus.ConnectingToDevice
                )
            }
            _viewEvents.post(QrCodeLoginViewEvents.NavigateToStatusScreen)
        }
    }

    private fun onFailed(errorType: QrCodeLoginErrorType, canTryAgain: Boolean) {
        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.Failed(errorType, canTryAgain)
            )
        }
    }

    private fun onConnectionEstablished(securityCode: String) {
        val canConfirmSecurityCode = initialState.loginType == QrCodeLoginType.LINK_A_DEVICE
        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.Connected(securityCode, canConfirmSecurityCode)
            )
        }
    }

    private fun onSigningIn() {
        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.SigningIn
            )
        }
    }

    // TODO. Implement in the logic related PR.
    private fun isValidQrCode(qrCode: String): Boolean {
        Timber.d("isValidQrCode: $qrCode")
        return false
    }

    // TODO. Implement in the logic related PR.
    private fun generateQrCodeData(): String {
        return "TODO"
    }
}
