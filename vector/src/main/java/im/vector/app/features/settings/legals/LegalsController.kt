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
import im.vector.app.features.discovery.IdentityServerPolicy
import im.vector.app.features.discovery.IdentityServerWithTerms
import im.vector.app.features.discovery.discoveryPolicyItem
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import javax.inject.Inject

class LegalsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<LegalsState>() {

    var listener: Listener? = null

    override fun buildModels(data: LegalsState) {
        buildAppSection()
        buildHomeserverSection(data)
        buildIdentityServerSection(data)
    }

    private fun buildAppSection() {
        settingsSectionTitleItem {
            id("appTitle")
            titleResId(R.string.legals_application_title)
        }

        // TODO
    }

    private fun buildHomeserverSection(data: LegalsState) {
        settingsSectionTitleItem {
            id("hsServerTitle")
            titleResId(R.string.legals_home_server_title)
        }

        buildPolicy("hs", data.homeServer)
    }

    private fun buildIdentityServerSection(data: LegalsState) {
        if (data.hasIdentityServer) {
            settingsSectionTitleItem {
                id("idServerTitle")
                titleResId(R.string.legals_identity_server_title)
            }

            buildPolicy("is", data.identityServer)
        }
    }

    private fun buildPolicy(tag: String, serverWithTerms: Async<IdentityServerWithTerms?>) {
        val host = this

        when (serverWithTerms) {
            Uninitialized,
            is Loading -> loadingItem {
                id("loading_$tag")
            }
            is Success -> {
                val policies = serverWithTerms()?.policies
                if (policies.isNullOrEmpty()) {
                    settingsInfoItem {
                        id("emptyPolicy")
                        helperText(host.stringProvider.getString(R.string.legals_no_policy_provided))
                    }
                } else {
                    policies.forEach { policy ->
                        discoveryPolicyItem {
                            id(policy.url)
                            name(policy.name)
                            url(policy.url)
                            clickListener { host.listener?.openPolicy(policy) }
                        }
                    }
                }
            }
            is Fail    -> {
                errorWithRetryItem {
                    id("errorRetry_$tag")
                    text(host.errorFormatter.toHumanReadable(serverWithTerms.error))
                    listener { host.listener?.onTapRetry() }
                }
            }
        }
    }

    interface Listener {
        fun onTapRetry()
        fun openPolicy(policy: IdentityServerPolicy)
    }
}
