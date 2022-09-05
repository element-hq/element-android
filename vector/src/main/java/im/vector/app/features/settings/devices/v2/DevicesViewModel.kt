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

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.settings.devices.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
        private val checkIfSessionIsInactiveUseCase: CheckIfSessionIsInactiveUseCase,
        getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val getEncryptionTrustLevelForDeviceUseCase: GetEncryptionTrustLevelForDeviceUseCase,
) : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DevicesViewModel, DevicesViewState> {
        override fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MavericksViewModelFactory<DevicesViewModel, DevicesViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: DevicesAction) {
        when(action) {
            is DevicesAction.MarkAsManuallyVerified -> handleMarkAsManuallyVerifiedAction()
        }
    }

    private fun handleMarkAsManuallyVerifiedAction() {
        // TODO implement when needed
    }
}

