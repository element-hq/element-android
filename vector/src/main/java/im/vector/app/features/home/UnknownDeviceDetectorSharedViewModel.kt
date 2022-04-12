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

package im.vector.app.features.home

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

data class UnknownDevicesState(
        val myMatrixItem: MatrixItem.UserItem? = null,
        val unknownSessions: Async<List<DeviceDetectionInfo>> = Uninitialized
) : MavericksState

data class DeviceDetectionInfo(
        val deviceInfo: DeviceInfo,
        val isNew: Boolean,
        val currentSessionTrust: Boolean
)

class UnknownDeviceDetectorSharedViewModel @AssistedInject constructor(@Assisted initialState: UnknownDevicesState,
                                                                       session: Session,
                                                                       private val vectorPreferences: VectorPreferences) :
    VectorViewModel<UnknownDevicesState, UnknownDeviceDetectorSharedViewModel.Action, EmptyViewEvents>(initialState) {

    sealed class Action : VectorViewModelAction {
        data class IgnoreDevice(val deviceIds: List<String>) : Action()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> {
        override fun create(initialState: UnknownDevicesState): UnknownDeviceDetectorSharedViewModel
    }

    companion object : MavericksViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> by hiltMavericksViewModelFactory()

    private val ignoredDeviceList = ArrayList<String>()

    init {

        val currentSessionTs = session.cryptoService().getCryptoDeviceInfo(session.myUserId)
                .firstOrNull { it.deviceId == session.sessionParams.deviceId }
                ?.firstTimeSeenLocalTs
                ?: System.currentTimeMillis()
        Timber.v("## Detector - Current Session first time seen $currentSessionTs")

        ignoredDeviceList.addAll(
                vectorPreferences.getUnknownDeviceDismissedList().also {
                    Timber.v("## Detector - Remembered ignored list $it")
                }
        )

        combine(
                session.flow().liveUserCryptoDevices(session.myUserId),
                session.flow().liveMyDevicesInfo(),
                session.flow().liveCrossSigningPrivateKeys()
        ) { cryptoList, infoList, pInfo ->
            //                    Timber.v("## Detector trigger ${cryptoList.map { "${it.deviceId} ${it.trustLevel}" }}")
//                    Timber.v("## Detector trigger canCrossSign ${pInfo.get().selfSigned != null}")
            infoList
                    .filter { info ->
                        // filter verified session, by checking the crypto device info
                        cryptoList.firstOrNull { info.deviceId == it.deviceId }?.isVerified?.not().orFalse()
                    }
                    // filter out ignored devices
                    .filter { !ignoredDeviceList.contains(it.deviceId) }
                    .sortedByDescending { it.lastSeenTs }
                    .map { deviceInfo ->
                        val deviceKnownSince = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }?.firstTimeSeenLocalTs ?: 0
                        DeviceDetectionInfo(
                                deviceInfo,
                                deviceKnownSince > currentSessionTs + 60_000, // short window to avoid false positive,
                                pInfo.getOrNull()?.selfSigned != null // adding this to pass distinct when cross sign change
                        )
                    }
        }
                .distinctUntilChanged()
                .execute { async ->
                    //                    Timber.v("## Detector trigger passed distinct")
                    copy(
                            myMatrixItem = session.getUser(session.myUserId)?.toMatrixItem(),
                            unknownSessions = async
                    )
                }

        session.flow().liveUserCryptoDevices(session.myUserId)
                .distinctUntilChanged()
                .sample(5_000)
                .onEach {
                    // If we have a new crypto device change, we might want to trigger refresh of device info
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                }
                .launchIn(viewModelScope)

        // trigger a refresh of lastSeen / last Ip
        session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
    }

    override fun handle(action: Action) {
        when (action) {
            is Action.IgnoreDevice -> {
                ignoredDeviceList.addAll(action.deviceIds)
                // local echo
                withState { state ->
                    state.unknownSessions.invoke()?.let { detectedSessions ->
                        val updated = detectedSessions.filter { !action.deviceIds.contains(it.deviceInfo.deviceId) }
                        setState {
                            copy(unknownSessions = Success(updated))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        vectorPreferences.storeUnknownDeviceDismissedList(ignoredDeviceList)
        super.onCleared()
    }
}
