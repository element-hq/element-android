/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import kotlin.time.Duration.Companion.seconds

abstract class VectorSessionsListViewModel<S : MavericksState, VA : VectorViewModelAction, VE : VectorViewEvents>(
        initialState: S,
        private val activeSessionHolder: ActiveSessionHolder,
        private val refreshDevicesUseCase: RefreshDevicesUseCase,
) : VectorViewModel<S, VA, VE>(initialState), VerificationService.Listener {

    private val refreshSource = PublishDataSource<Unit>()
    private val refreshThrottleDelayMs = 4.seconds.inWholeMilliseconds

    init {
        addVerificationListener()
        observeRefreshSource()
    }

    override fun onCleared() {
        removeVerificationListener()
        super.onCleared()
    }

    private fun addVerificationListener() {
        activeSessionHolder.getSafeActiveSession()
                ?.cryptoService()
                ?.verificationService()
                ?.addListener(this)
    }

    private fun removeVerificationListener() {
        activeSessionHolder.getSafeActiveSession()
                ?.cryptoService()
                ?.verificationService()
                ?.removeListener(this)
    }

    private fun observeRefreshSource() {
        refreshSource.stream()
                .throttleFirst(refreshThrottleDelayMs)
                .onEach { refreshDevicesUseCase.execute() }
                .launchIn(viewModelScope)
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx.state == VerificationTxState.Verified) {
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
