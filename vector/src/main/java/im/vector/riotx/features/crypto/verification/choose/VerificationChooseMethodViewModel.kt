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
package im.vector.riotx.features.crypto.verification.choose

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.VerificationService
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.crypto.sas.VerificationTransaction
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet

data class VerificationChooseMethodViewState(
        val otherUserId: String = "",
        val transactionId: String = "",
        val otherCanShowQrCode: Boolean = false,
        val otherCanScanQrCode: Boolean = false,
        val qrCodeText: String? = null,
        val SASModeAvailable: Boolean = false
) : MvRxState

class VerificationChooseMethodViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationChooseMethodViewState,
        private val session: Session
) : VectorViewModel<VerificationChooseMethodViewState, EmptyAction, EmptyViewEvents>(initialState), VerificationService.VerificationListener {

    override fun transactionCreated(tx: VerificationTransaction) {}

    override fun transactionUpdated(tx: VerificationTransaction) {}

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->
        val pvr = session.getVerificationService().getExistingVerificationRequest(state.otherUserId, state.transactionId)

        setState {
            copy(
                    otherCanShowQrCode = pvr?.hasMethod(VerificationMethod.QR_CODE_SHOW) ?: false,
                    otherCanScanQrCode = pvr?.hasMethod(VerificationMethod.QR_CODE_SCAN) ?: false,
                    qrCodeText = pvr?.qrCodeText,
                    SASModeAvailable = pvr?.hasMethod(VerificationMethod.SAS) ?: false
            )
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationChooseMethodViewState): VerificationChooseMethodViewModel
    }

    init {
        session.getVerificationService().addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        session.getVerificationService().removeListener(this)
    }

    companion object : MvRxViewModelFactory<VerificationChooseMethodViewModel, VerificationChooseMethodViewState> {
        override fun create(viewModelContext: ViewModelContext, state: VerificationChooseMethodViewState): VerificationChooseMethodViewModel? {
            val fragment: VerificationChooseMethodFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.verificationChooseMethodViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationChooseMethodViewState? {
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            val pvr = session.getVerificationService().getExistingVerificationRequest(args.otherUserId, args.verificationId)

            return VerificationChooseMethodViewState(otherUserId = args.otherUserId,
                    transactionId = args.verificationId ?: "",
                    otherCanShowQrCode = pvr?.hasMethod(VerificationMethod.QR_CODE_SHOW) ?: false,
                    otherCanScanQrCode = pvr?.hasMethod(VerificationMethod.QR_CODE_SCAN) ?: false,
                    qrCodeText = pvr?.qrCodeText,
                    SASModeAvailable = pvr?.hasMethod(VerificationMethod.SAS) ?: false
            )
        }
    }

    override fun handle(action: EmptyAction) {}
}
