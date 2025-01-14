/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.legals

import android.content.res.Resources
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.ServerAndPolicies
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.discovery.discoveryPolicyItem
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class LegalsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val resources: Resources,
        private val elementLegals: ElementLegals,
        private val errorFormatter: ErrorFormatter,
        private val flavorLegals: FlavorLegals,
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
            titleResId(CommonStrings.legals_application_title)
        }

        buildPolicies("el", elementLegals.getData())
    }

    private fun buildHomeserverSection(data: LegalsState) {
        settingsSectionTitleItem {
            id("hsServerTitle")
            titleResId(CommonStrings.legals_home_server_title)
        }

        buildPolicyAsync("hs", data.homeServer)
    }

    private fun buildIdentityServerSection(data: LegalsState) {
        if (data.hasIdentityServer) {
            settingsSectionTitleItem {
                id("idServerTitle")
                titleResId(CommonStrings.legals_identity_server_title)
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
                        helperText(host.stringProvider.getString(CommonStrings.legals_no_policy_provided))
                    }
                } else {
                    buildPolicies(tag, policies)
                }
            }
            is Fail -> {
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
            titleResId(CommonStrings.legals_third_party_notices)
        }

        discoveryPolicyItem {
            id("eltpn1")
            name(host.stringProvider.getString(CommonStrings.settings_third_party_notices))
            clickListener { host.listener?.openThirdPartyNotice() }
        }
        // Only on Gplay
        if (flavorLegals.hasThirdPartyNotices()) {
            discoveryPolicyItem {
                id("eltpn2")
                name(host.stringProvider.getString(CommonStrings.settings_other_third_party_notices))
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
