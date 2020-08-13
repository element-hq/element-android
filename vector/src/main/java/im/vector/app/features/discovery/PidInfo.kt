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

package im.vector.app.features.discovery

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid

data class PidInfo(
        // Retrieved from the homeserver
        val threePid: ThreePid,
        // Retrieved from IdentityServer, or transient state
        val isShared: Async<SharedState>,
        // Contains information about a current request to submit the token (for instance SMS code received by SMS)
        // Or a current binding finalization, for email
        val finalRequest: Async<Unit> = Uninitialized
)
