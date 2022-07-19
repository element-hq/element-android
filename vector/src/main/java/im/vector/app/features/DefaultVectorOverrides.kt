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

package im.vector.app.features

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface VectorOverrides {
    val forceDialPad: Flow<Boolean>
    val forceLoginFallback: Flow<Boolean>
    val forceHomeserverCapabilities: Flow<HomeserverCapabilitiesOverride>?
}

data class HomeserverCapabilitiesOverride(
        val canChangeDisplayName: Boolean?,
        val canChangeAvatar: Boolean?
)

class DefaultVectorOverrides : VectorOverrides {
    override val forceDialPad = flowOf(false)
    override val forceLoginFallback = flowOf(false)
    override val forceHomeserverCapabilities: Flow<HomeserverCapabilitiesOverride>? = null
}
