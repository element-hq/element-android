/*
 * Copyright (c) 2024 New Vector Ltd
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

package im.vector.app.features.analytics.impl

import im.vector.app.ActiveSessionDataSource
import im.vector.app.features.analytics.plan.SuperProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Gathers the super properties that are static to this platform or
 * that can be automatically resolved from the current session.
 */
class AutoSuperPropertiesFlowProvider @Inject constructor(
        activeSessionDataSource: ActiveSessionDataSource,
) {

    val superPropertiesFlow: Flow<SuperProperties> = activeSessionDataSource.stream()
            .map { session ->
                SuperProperties(
                        appPlatform = SuperProperties.AppPlatform.EA,
                        cryptoSDK = SuperProperties.CryptoSDK.Rust,
                        cryptoSDKVersion = session.getOrNull()?.cryptoService()?.getCryptoVersion(false)
                )
            }
            .distinctUntilChanged()
}
