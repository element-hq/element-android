/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login.terms

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms

data class LoginTermsViewState(
        val localizedFlowDataLoginTermsChecked: List<LocalizedFlowDataLoginTermsChecked>
) : MavericksState {
    fun check(data: LocalizedFlowDataLoginTerms) {
        localizedFlowDataLoginTermsChecked.find { it.localizedFlowDataLoginTerms == data }?.checked = true
    }

    fun uncheck(data: LocalizedFlowDataLoginTerms) {
        localizedFlowDataLoginTermsChecked.find { it.localizedFlowDataLoginTerms == data }?.checked = false
    }

    fun allChecked(): Boolean {
        return localizedFlowDataLoginTermsChecked.all { it.checked }
    }
}
