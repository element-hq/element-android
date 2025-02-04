/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.legals

import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.settings.VectorSettingsUrls
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class ElementLegals @Inject constructor(
        private val stringProvider: StringProvider
) {
    /**
     * Use ServerPolicy model.
     */
    fun getData(): List<ServerPolicy> {
        return listOf(
                ServerPolicy(stringProvider.getString(CommonStrings.settings_copyright), VectorSettingsUrls.COPYRIGHT),
                ServerPolicy(stringProvider.getString(CommonStrings.settings_acceptable_use_policy), VectorSettingsUrls.ACCEPTABLE_USE_POLICY),
                ServerPolicy(stringProvider.getString(CommonStrings.settings_privacy_policy), VectorSettingsUrls.PRIVACY_POLICY)
        )
    }
}
