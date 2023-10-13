/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.workmanager

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.internal.session.homeserver.HomeServerCapabilitiesDataSource
import javax.inject.Inject

@Suppress("RedundantIf", "IfThenToElvis")
internal class DefaultWorkManagerConfig @Inject constructor(
        private val credentials: Credentials,
        private val homeServerCapabilitiesDataSource: HomeServerCapabilitiesDataSource,
) : WorkManagerConfig {
    override fun withNetworkConstraint(): Boolean {
        val disableNetworkConstraint = homeServerCapabilitiesDataSource.getHomeServerCapabilities()?.disableNetworkConstraint
        return if (disableNetworkConstraint != null) {
            // Boolean `io.element.disable_network_constraint` explicitly set in the .well-known file
            disableNetworkConstraint.not()
        } else if (credentials.discoveryInformation?.disableNetworkConstraint == true) {
            // Boolean `io.element.disable_network_constraint` explicitly set to `true` in the login response
            false
        } else {
            // Default, use the Network constraint
            true
        }
    }
}
