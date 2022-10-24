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
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.session.ConfigureAndStartSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.rendezvous.Rendezvous
import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason
import org.matrix.android.sdk.api.rendezvous.model.RendezvousError
import timber.log.Timber

class QrCodeLoginViewModel @AssistedInject constructor(
        @Assisted private val initialState: QrCodeLoginViewState,
        private val authenticationService: AuthenticationService,
        private val activeSessionHolder: ActiveSessionHolder,
        private val configureAndStartSessionUseCase: ConfigureAndStartSessionUseCase,
) : VectorViewModel<QrCodeLoginViewState, QrCodeLoginAction, QrCodeLoginViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<QrCodeLoginViewModel, QrCodeLoginViewState> {
        override fun create(initialState: QrCodeLoginViewState): QrCodeLoginViewModel
    }

    companion object : MavericksViewModelFactory<QrCodeLoginViewModel, QrCodeLoginViewState> by hiltMavericksViewModelFactory() {
        val TAG: String = QrCodeLoginViewModel::class.java.simpleName
    }

    override fun handle(action: QrCodeLoginAction) {
        when (action) {
            is QrCodeLoginAction.OnQrCodeScanned -> handleOnQrCodeScanned(action)
            QrCodeLoginAction.GenerateQrCode -> handleQrCodeViewStarted()
            QrCodeLoginAction.ShowQrCode -> handleShowQrCode()
            QrCodeLoginAction.TryAgain -> handleTryAgain()
        }
    }

    private fun handleTryAgain() {
        setState {
            copy(
                    connectionStatus = null
            )
        }
        _viewEvents.post(QrCodeLoginViewEvents.NavigateToInitialScreen)
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
        Timber.tag(TAG).d("Scanned code of length ${action.qrCode.length}")

        val rendezvous = try { Rendezvous.buildChannelFromCode(action.qrCode) } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "Error occurred during sign in")
            if (t is RendezvousError) {
                onFailed(t.reason)
            } else {
                onFailed(RendezvousFailureReason.Unknown)
            }
            return
        }

        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.ConnectingToDevice
            )
        }

        _viewEvents.post(QrCodeLoginViewEvents.NavigateToStatusScreen)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val confirmationCode = rendezvous.startAfterScanningCode()
                Timber.tag(TAG).i("Established secure channel with checksum: $confirmationCode")

                onConnectionEstablished(confirmationCode)

                val session = rendezvous.waitForLoginOnNewDevice(authenticationService)
                onSigningIn()

                activeSessionHolder.setActiveSession(session)
                authenticationService.reset()
                configureAndStartSessionUseCase.execute(session)

                rendezvous.completeVerificationOnNewDevice(session)

                _viewEvents.post(QrCodeLoginViewEvents.NavigateToHomeScreen)
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Error occurred during sign in")
                if (t is RendezvousError) {
                    onFailed(t.reason)
                } else {
                    onFailed(RendezvousFailureReason.Unknown)
                }
            }
        }
    }

    private fun onFailed(reason: RendezvousFailureReason) {
        _viewEvents.post(QrCodeLoginViewEvents.NavigateToStatusScreen)

        setState {
            copy(
                    connectionStatus = QrCodeLoginConnectionStatus.Failed(reason, reason.canRetry)
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

    /**
     * QR code generation is not currently supported and this is a placeholder for future
     * functionality.
     */
    private fun generateQrCodeData(): String {
        return "NOT SUPPORTED"
    }
}
