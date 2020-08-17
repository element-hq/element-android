/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.settings.crosssigning

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.crosssigning.isVerified
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.rx.rx

class CrossSigningSettingsViewModel @AssistedInject constructor(@Assisted private val initialState: CrossSigningSettingsViewState,
                                                                private val session: Session)
    : VectorViewModel<CrossSigningSettingsViewState, CrossSigningSettingsAction, CrossSigningSettingsViewEvents>(initialState) {

    init {
        Observable.combineLatest<List<DeviceInfo>, Optional<MXCrossSigningInfo>, Pair<List<DeviceInfo>, Optional<MXCrossSigningInfo>>>(
                session.rx().liveMyDevicesInfo(),
                session.rx().liveCrossSigningInfo(session.myUserId),
                BiFunction { myDevicesInfo, mxCrossSigningInfo ->
                    myDevicesInfo to mxCrossSigningInfo
                }
        )
                .execute { data ->
                    val crossSigningKeys = data.invoke()?.second?.getOrNull()
                    val xSigningIsEnableInAccount = crossSigningKeys != null
                    val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
                    val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()

                    copy(
                            crossSigningInfo = crossSigningKeys,
                            xSigningIsEnableInAccount = xSigningIsEnableInAccount,
                            xSigningKeysAreTrusted = xSigningKeysAreTrusted,
                            xSigningKeyCanSign = xSigningKeyCanSign
                    )
                }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CrossSigningSettingsViewState): CrossSigningSettingsViewModel
    }

    override fun handle(action: CrossSigningSettingsAction) {
        // No op for the moment
        // when (action) {
        // }.exhaustive
    }

    companion object : MvRxViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CrossSigningSettingsViewState): CrossSigningSettingsViewModel? {
            val fragment: CrossSigningSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }
}
