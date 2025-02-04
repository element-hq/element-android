/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login.terms

import com.airbnb.epoxy.TypedEpoxyController
import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms
import javax.inject.Inject

class PolicyController @Inject constructor() : TypedEpoxyController<List<LocalizedFlowDataLoginTermsChecked>>() {

    var listener: PolicyControllerListener? = null

    var horizontalPadding: Int? = null
    var homeServer: String? = null

    override fun buildModels(data: List<LocalizedFlowDataLoginTermsChecked>) {
        val host = this
        data.forEach { entry ->
            policyItem {
                id(entry.localizedFlowDataLoginTerms.policyName)
                checked(entry.checked)
                horizontalPadding(host.horizontalPadding)
                title(entry.localizedFlowDataLoginTerms.localizedName)
                subtitle(host.homeServer)
                clickListener { host.listener?.openPolicy(entry.localizedFlowDataLoginTerms) }
                checkChangeListener { _, isChecked ->
                    host.listener?.setChecked(entry.localizedFlowDataLoginTerms, isChecked)
                }
            }
        }
    }

    interface PolicyControllerListener {
        fun setChecked(localizedFlowDataLoginTerms: LocalizedFlowDataLoginTerms, isChecked: Boolean)
        fun openPolicy(localizedFlowDataLoginTerms: LocalizedFlowDataLoginTerms)
    }
}
