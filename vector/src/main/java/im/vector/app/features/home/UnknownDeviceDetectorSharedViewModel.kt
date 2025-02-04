/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.session.clientinfo.DeleteUnusedClientInformationUseCase
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

data class UnknownDevicesState(
        val myMatrixItem: MatrixItem.UserItem,
        val unknownSessions: Async<List<DeviceDetectionInfo>> = Uninitialized
) : MavericksState

data class DeviceDetectionInfo(
        val deviceInfo: DeviceInfo,
        val isNew: Boolean,
        val currentSessionTrust: Boolean
)

class UnknownDeviceDetectorSharedViewModel @AssistedInject constructor(
        @Assisted initialState: UnknownDevicesState,
        session: Session,
        clock: Clock,
        private val shouldShowUnverifiedSessionsAlertUseCase: ShouldShowUnverifiedSessionsAlertUseCase,
        private val setUnverifiedSessionsAlertShownUseCase: SetUnverifiedSessionsAlertShownUseCase,
        private val isNewLoginAlertShownUseCase: IsNewLoginAlertShownUseCase,
        private val setNewLoginAlertShownUseCase: SetNewLoginAlertShownUseCase,
        private val deleteUnusedClientInformationUseCase: DeleteUnusedClientInformationUseCase,
) : VectorViewModel<UnknownDevicesState, UnknownDeviceDetectorSharedViewModel.Action, EmptyViewEvents>(initialState) {

    sealed class Action : VectorViewModelAction {
        data class IgnoreDevice(val deviceIds: List<String>) : Action()
        data class IgnoreNewLogin(val deviceIds: List<String>) : Action()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> {
        override fun create(initialState: UnknownDevicesState): UnknownDeviceDetectorSharedViewModel
    }

    companion object : MavericksViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> by hiltMavericksViewModelFactory() {
        override fun initialState(viewModelContext: ViewModelContext): UnknownDevicesState {
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()

            return UnknownDevicesState(
                    myMatrixItem = session.getUserOrDefault(session.myUserId).toMatrixItem()
            )
        }
    }

    init {

        combine(
                session.flow().liveUserCryptoDevices(session.myUserId),
                session.flow().liveMyDevicesInfo(),
                session.flow().liveCrossSigningPrivateKeys(),
        ) { cryptoList, infoList, pInfo ->
            Timber.v("## Detector trigger ${cryptoList.map { "${it.deviceId} ${it.trustLevel}" }}")
            Timber.v("## Detector trigger canCrossSign ${pInfo.get().selfSigned != null}")

            deleteUnusedClientInformation(infoList)

            val currentSessionTs = session.cryptoService().getCryptoDeviceInfo(session.myUserId)
                    .firstOrNull { it.deviceId == session.sessionParams.deviceId }
                    ?.firstTimeSeenLocalTs
                    ?: clock.epochMillis()
            Timber.v("## Detector - Current Session first time seen $currentSessionTs")

            infoList
                    .asSequence()
                    .filter {
                        // filter out own device
                        session.sessionParams.deviceId != it.deviceId
                    }
                    .filter { info ->
                        val matchingDeviceWithKeys = cryptoList.firstOrNull { it.deviceId == info.deviceId }
                        if (matchingDeviceWithKeys == null) {
                            // filter out verified sessions or those which do not support encryption (i.e. without crypto info)
                            false
                        } else {
                            // Only report unverified
                            !matchingDeviceWithKeys.isVerified
                        }
                    }
                    // filter out ignored devices
                    .filter { shouldShowUnverifiedSessionsAlertUseCase.execute(it.deviceId) }
                    .sortedByDescending { it.lastSeenTs }
                    .map { deviceInfo ->
                        val deviceKnownSince = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }?.firstTimeSeenLocalTs ?: 0
                        val isNew = isNewLoginAlertShownUseCase.execute(deviceInfo.deviceId).not() && deviceKnownSince > currentSessionTs

                        DeviceDetectionInfo(
                                deviceInfo,
                                isNew,
                                pInfo.getOrNull()?.selfSigned != null // adding this to pass distinct when cross sign change
                        )
                    }
                    .toList()
        }
                .distinctUntilChanged()
                .execute { async ->
                    Timber.v("## Detector trigger passed distinct ${async.invoke()}")
                    copy(
                            myMatrixItem = session.getUserOrDefault(session.myUserId).toMatrixItem(),
                            unknownSessions = async
                    )
                }

        session.flow().liveUserCryptoDevices(session.myUserId)
                .distinctUntilChanged()
                .sample(5_000)
                .onEach {
                    // If we have a new crypto device change, we might want to trigger refresh of device info
                    tryOrNull { session.cryptoService().fetchDevicesList() }
                }
                .launchIn(viewModelScope)

        // trigger a refresh of lastSeen / last Ip
        viewModelScope.launch {
            tryOrNull { session.cryptoService().fetchDevicesList() }
        }
    }

    private fun deleteUnusedClientInformation(deviceFullInfoList: List<DeviceInfo>) {
        viewModelScope.launch {
            deleteUnusedClientInformationUseCase.execute(deviceFullInfoList)
        }
    }

    override fun handle(action: Action) {
        when (action) {
            is Action.IgnoreDevice -> {
                setUnverifiedSessionsAlertShownUseCase.execute(action.deviceIds)
            }
            is Action.IgnoreNewLogin -> {
                setNewLoginAlertShownUseCase.execute(action.deviceIds)
            }
        }
    }
}
