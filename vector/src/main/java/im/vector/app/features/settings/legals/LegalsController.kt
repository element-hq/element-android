/*
 * Copyright (c) 2021 New Vector Ltd
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
package im.vector.app.features.settings.legals

import android.content.res.Resources
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.ServerAndPolicies
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.discovery.discoveryPolicyItem
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import javax.inject.Inject

class LegalsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val resources: Resources,
        private val elementLegals: ElementLegals,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<LegalsState>() {

    var listener: Listener? = null

    override fun buildModels(data: LegalsState) {
        buildAppSection()
        buildHomeserverSection(data)
        buildIdentityServerSection(data)
        buildThirdPartyNotices()
    }

    private fun buildAppSection() {
        settingsSectionTitleItem {
            id("appTitle")
            titleResId(R.string.legals_application_title)
        }

        buildPolicies("el", elementLegals.getData())
    }

    private fun buildHomeserverSection(data: LegalsState) {
        settingsSectionTitleItem {
            id("hsServerTitle")
            titleResId(R.string.legals_home_server_title)
        }

        buildPolicyAsync("hs", data.homeServer)
    }

    private fun buildIdentityServerSection(data: LegalsState) {
        if (data.hasIdentityServer) {
            settingsSectionTitleItem {
                id("idServerTitle")
                titleResId(R.string.legals_identity_server_title)
            }

            buildPolicyAsync("is", data.identityServer)
        }
    }

    private fun buildPolicyAsync(tag: String, serverAndPolicies: Async<ServerAndPolicies?>) {
        val host = this

        when (serverAndPolicies) {
            Uninitialized,
            is Loading -> loadingItem {
                id("loading_$tag")
            }
            is Success -> {
                val policies = serverAndPolicies()?.policies
                if (policies.isNullOrEmpty()) {
                    settingsInfoItem {
                        id("emptyPolicy")
                        helperText(host.stringProvider.getString(R.string.legals_no_policy_provided))
                    }
                } else {
                    buildPolicies(tag, policies)
                }
            }
            is Fail    -> {
                errorWithRetryItem {
                    id("errorRetry_$tag")
                    text(host.errorFormatter.toHumanReadable(serverAndPolicies.error))
                    listener { host.listener?.onTapRetry() }
                }
            }
        }
    }

    private fun buildPolicies(tag: String, policies: List<ServerPolicy>) {
        val host = this

        policies.forEach { policy ->
            discoveryPolicyItem {
                id(tag + policy.url)
                name(policy.name)
                url(policy.url.takeIf { it.startsWith("http") })
                clickListener { host.listener?.openPolicy(policy) }
            }
        }
    }

    private fun buildThirdPartyNotices() {
        val host = this
        settingsSectionTitleItem {
            id("thirdTitle")
            titleResId(R.string.legals_third_party_notices)
        }

        discoveryPolicyItem {
            id("eltpn1")
            name(host.stringProvider.getString(R.string.settings_third_party_notices))
            clickListener { host.listener?.openThirdPartyNotice() }
        }
        // Only on Gplay
        if (resources.getBoolean(R.bool.isGplay)) {
            discoveryPolicyItem {
                id("eltpn2")
                name(host.stringProvider.getString(R.string.settings_other_third_party_notices))
                clickListener { host.listener?.openThirdPartyNoticeGplay() }
            }
        }
    }

    interface Listener {
        fun onTapRetry()
        fun openPolicy(policy: ServerPolicy)
        fun openThirdPartyNotice()
        fun openThirdPartyNoticeGplay()
    }
}
