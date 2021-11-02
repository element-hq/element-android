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

package im.vector.app.features.settings.threepids

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.utils.ReadOnceTrue
import org.matrix.android.sdk.api.session.identity.ThreePid

data class ThreePidsSettingsViewState(
        val uiState: ThreePidsSettingsUiState = ThreePidsSettingsUiState.Idle,
        val isLoading: Boolean = false,
        val threePids: Async<List<ThreePid>> = Uninitialized,
        val pendingThreePids: Async<List<ThreePid>> = Uninitialized,
        val msisdnValidationRequests: Map<String, Async<Unit>> = emptyMap(),
        val editTextReinitiator: ReadOnceTrue = ReadOnceTrue(),
        val msisdnValidationReinitiator: Map<ThreePid, ReadOnceTrue> = emptyMap()
) : MavericksState
