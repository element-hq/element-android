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

package im.vector.riotx.features.home

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.NoOpMatrixCallback
import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.store.PrivateKeysInfo
import im.vector.matrix.rx.rx
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.features.settings.VectorPreferences
import io.reactivex.Observable
import io.reactivex.functions.Function3
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class UnknownDevicesState(
        val myMatrixItem: MatrixItem.UserItem? = null,
        val unknownSessions: Async<List<DeviceDetectionInfo>> = Uninitialized
) : MvRxState

data class DeviceDetectionInfo(
        val deviceInfo: DeviceInfo,
        val isNew: Boolean,
        val currentSessionTrust: Boolean
)

class UnknownDeviceDetectorSharedViewModel(
        session: Session,
        private val vectorPreferences: VectorPreferences,
        initialState: UnknownDevicesState)
    : VectorViewModel<UnknownDevicesState, UnknownDeviceDetectorSharedViewModel.Action, EmptyViewEvents>(initialState) {

    sealed class Action : VectorViewModelAction {
        data class IgnoreDevice(val deviceIds: List<String>) : Action()
    }

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

        Observable.combineLatest<List<CryptoDeviceInfo>, List<DeviceInfo>, Optional<PrivateKeysInfo>, List<DeviceDetectionInfo>>(
                session.rx().liveUserCryptoDevices(session.myUserId),
                session.rx().liveMyDeviceInfo(),
                session.rx().liveCrossSigningPrivateKeys(),
                Function3 { cryptoList, infoList, pInfo ->
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
        )
                .distinctUntilChanged()
                .execute { async ->
//                    Timber.v("## Detector trigger passed distinct")
                    copy(
                            myMatrixItem = session.getUser(session.myUserId)?.toMatrixItem(),
                            unknownSessions = async
                    )
                }

        session.rx().liveUserCryptoDevices(session.myUserId)
                .distinct()
                .throttleLast(5_000, TimeUnit.MILLISECONDS)
                .subscribe {
                    // If we have a new crypto device change, we might want to trigger refresh of device info
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                }.disposeOnClear()

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

    companion object : MvRxViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> {

        override fun create(viewModelContext: ViewModelContext, state: UnknownDevicesState): UnknownDeviceDetectorSharedViewModel? {
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            return UnknownDeviceDetectorSharedViewModel(session, VectorPreferences(viewModelContext.activity()), state)
        }
    }
}
