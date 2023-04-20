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

package im.vector.app.features.settings.devices.v2

import com.airbnb.mvrx.MavericksState
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.utils.PublishDataSource
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import kotlin.time.Duration.Companion.seconds

abstract class VectorSessionsListViewModel<S : MavericksState, VA : VectorViewModelAction, VE : VectorViewEvents>(
        initialState: S,
        private val activeSessionHolder: ActiveSessionHolder,
        private val refreshDevicesUseCase: RefreshDevicesUseCase,
) : VectorViewModel<S, VA, VE>(initialState), VerificationService.Listener {

    private val refreshSource = PublishDataSource<Unit>()
    private val refreshThrottleDelayMs = 4.seconds.inWholeMilliseconds

    init {
//        addVerificationListener()
        observeRefreshSource()
        activeSessionHolder.getSafeActiveSession()
                ?.cryptoService()
                ?.verificationService()
                ?.requestEventFlow()
                ?.onEach {
                    when (it) {
                        is VerificationEvent.RequestAdded -> verificationRequestCreated(it.request)
                        is VerificationEvent.RequestUpdated -> verificationRequestUpdated(it.request)
                        is VerificationEvent.TransactionAdded -> transactionCreated(it.transaction)
                        is VerificationEvent.TransactionUpdated -> transactionUpdated(it.transaction)
                    }
                }
                ?.launchIn(viewModelScope)
    }

//    override fun onCleared() {
//        super.onCleared()
//    }

//    private fun addVerificationListener() {
//        activeSessionHolder.getSafeActiveSession()
//                ?.cryptoService()
//                ?.verificationService()
//                ?.addListener(this)
//    }

//    private fun removeVerificationListener() {
//        activeSessionHolder.getSafeActiveSession()
//                ?.cryptoService()
//                ?.verificationService()
//                ?.removeListener(this)
//    }

    private fun observeRefreshSource() {
        refreshSource.stream()
                .throttleFirst(refreshThrottleDelayMs)
                .onEach { refreshDevicesUseCase.execute() }
                .launchIn(viewModelScope)
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx.isSuccessful()) {
            refreshDeviceList()
        }
    }

    /**
     * Force the refresh of the devices list.
     * The devices list is the list of the devices where the user is logged in.
     * It can be any mobile devices, and any browsers.
     */
    fun refreshDeviceList() {
        refreshSource.post(Unit)
    }
}
