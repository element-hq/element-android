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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            QrCodeLoginAction.QrCodeViewStarted -> handleQrCodeViewStarted()
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

        // TODO. UI test purpose. Fixme remove!
        viewModelScope.launch {
            delay(3000)
            onFailed(QrCodeLoginErrorType.TIMEOUT, false)
            delay(3000)
            onConnectionEstablished("1234-ABCD-5678-EFGH")
            delay(3000)
            onSigningIn()
            delay(3000)
            onFailed(QrCodeLoginErrorType.DEVICE_IS_NOT_SUPPORTED, true)
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
        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.Connected(securityCode)
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

    /**
     * TODO. UI test purpose. Fixme accordingly.
     */
    private fun isValidQrCode(qrCode: String): Boolean {
        return qrCode.startsWith("http")
    }

    /**
     * TODO. UI test purpose. Fixme accordingly.
     */
    private fun generateQrCodeData(): String {
        return "https://element.io"
    }
}
