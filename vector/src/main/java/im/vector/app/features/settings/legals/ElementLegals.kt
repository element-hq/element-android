/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.legals

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.settings.VectorSettingsUrls
import javax.inject.Inject

class ElementLegals @Inject constructor(
        private val stringProvider: StringProvider
) {
    /**
     * Use ServerPolicy model.
     */
    fun getData(): List<ServerPolicy> {
        return listOf(
                ServerPolicy(stringProvider.getString(R.string.settings_copyright), VectorSettingsUrls.COPYRIGHT),
                ServerPolicy(stringProvider.getString(R.string.settings_app_term_conditions), VectorSettingsUrls.TAC),
                ServerPolicy(stringProvider.getString(R.string.settings_privacy_policy), VectorSettingsUrls.PRIVACY_POLICY)
        )
    }
}
