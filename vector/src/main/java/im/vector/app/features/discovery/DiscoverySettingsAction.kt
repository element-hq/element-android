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

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.identity.ThreePid

sealed class DiscoverySettingsAction : VectorViewModelAction {
    object RetrieveBinding : DiscoverySettingsAction()
    object Refresh : DiscoverySettingsAction()

    object DisconnectIdentityServer : DiscoverySettingsAction()
    data class ChangeIdentityServer(val url: String) : DiscoverySettingsAction()
    data class UpdateUserConsent(val newConsent: Boolean) : DiscoverySettingsAction()
    data class RevokeThreePid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class ShareThreePid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class FinalizeBind3pid(val threePid: ThreePid) : DiscoverySettingsAction()
    data class SubmitMsisdnToken(val threePid: ThreePid.Msisdn, val code: String) : DiscoverySettingsAction()
    data class CancelBinding(val threePid: ThreePid) : DiscoverySettingsAction()
    data class SetPoliciesExpandState(val expanded: Boolean) : DiscoverySettingsAction()
}
