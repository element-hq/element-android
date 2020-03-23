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
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.singleBuilder
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import io.reactivex.android.schedulers.AndroidSchedulers

data class UnknownDevicesState(
        val unknownSessions: Async<List<DeviceInfo>?> = Uninitialized,
        val canCrossSign: Boolean = false
) : MvRxState

class UnknownDeviceDetectorSharedViewModel(session: Session, initialState: UnknownDevicesState) : VectorViewModel<UnknownDevicesState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        session.rx().liveUserCryptoDevices(session.myUserId)
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap { deviceList ->
                    singleBuilder<DevicesListResponse> {
                        session.cryptoService().getDevicesList(it)
                        NoOpCancellable
                    }.map { resp ->
                        resp.devices?.filter { info ->
                            deviceList.firstOrNull { info.deviceId == it.deviceId }?.isVerified?.not() ?: false
                        }?.sortedByDescending { it.lastSeenTs }
                    }
                            .toObservable()
                }
                .execute { async ->
                    copy(unknownSessions = async)
                }

        session.rx().liveCrossSigningInfo(session.myUserId)
                .execute {
                    copy(canCrossSign = session.cryptoService().crossSigningService().canCrossSign())
                }
    }

    override fun handle(action: EmptyAction) {}

    companion object : MvRxViewModelFactory<UnknownDeviceDetectorSharedViewModel, UnknownDevicesState> {
        override fun create(viewModelContext: ViewModelContext, state: UnknownDevicesState): UnknownDeviceDetectorSharedViewModel? {
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            return UnknownDeviceDetectorSharedViewModel(session, state)
        }
    }
}
