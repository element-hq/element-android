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

import android.content.Context
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.rendezvous.Rendezvous
import org.matrix.android.sdk.internal.rendezvous.RendezvousFailureReason
import timber.log.Timber

class QrCodeLoginViewModel @AssistedInject constructor(
        @Assisted private val initialState: QrCodeLoginViewState,
        private val applicationContext: Context,
        private val authenticationService: AuthenticationService,
        private val activeSessionHolder: ActiveSessionHolder,
) : VectorViewModel<QrCodeLoginViewState, QrCodeLoginAction, QrCodeLoginViewEvents>(initialState) {
    val TAG: String = QrCodeLoginViewModel::class.java.simpleName

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
        Timber.tag(TAG).d("Scanned code: ${action.qrCode}")

        val rendezvous = Rendezvous.buildChannelFromCode(action.qrCode) { reason ->
            Timber.tag(TAG).d("Rendezvous cancelled: $reason")
            onFailed(reason)
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
                confirmationCode?.let {
                    onConnectionEstablished(it)
                    val session = rendezvous.waitForLoginOnNewDevice(authenticationService)
                    onSigningIn()
                    session?.let {
                        activeSessionHolder.setActiveSession(session)
                        authenticationService.reset()

                        session.configureAndStart(applicationContext)

                        rendezvous.completeVerificationOnNewDevice(session)

                        _viewEvents.post(QrCodeLoginViewEvents.NavigateToHomeScreen)
                    }
                }
            } catch (failure: Throwable) {
                Timber.tag(TAG).e(failure, "Error occurred during sign in")
                onFailed(RendezvousFailureReason.Unknown)
            }
        }
    }

    private fun onFailed(reason: RendezvousFailureReason) {
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
     * TODO. UI test purpose. Fixme accordingly.
     */
    private fun generateQrCodeData(): String {
        return "https://element.io"
    }
}
