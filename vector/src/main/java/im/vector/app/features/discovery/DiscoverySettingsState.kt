/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.discovery

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class DiscoverySettingsState(
        val identityServer: Async<ServerAndPolicies?> = Uninitialized,
        val emailList: Async<List<PidInfo>> = Uninitialized,
        val phoneNumbersList: Async<List<PidInfo>> = Uninitialized,
        // Can be true if terms are updated
        val termsNotSigned: Boolean = false,
        val userConsent: Boolean = false,
        val isIdentityPolicyUrlsExpanded: Boolean = false
) : MavericksState
