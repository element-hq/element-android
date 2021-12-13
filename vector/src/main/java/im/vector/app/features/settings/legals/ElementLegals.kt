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

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.ServerPolicy
import im.vector.app.features.settings.VectorSettingsUrls
import javax.inject.Inject

class ElementLegals @Inject constructor(
        private val stringProvider: StringProvider
) {
    /**
     * Use ServerPolicy model
     */
    fun getData(): List<ServerPolicy> {
        return listOf(
                ServerPolicy(stringProvider.getString(R.string.settings_copyright), VectorSettingsUrls.COPYRIGHT),
                ServerPolicy(stringProvider.getString(R.string.settings_app_term_conditions), VectorSettingsUrls.TAC),
                ServerPolicy(stringProvider.getString(R.string.settings_privacy_policy), VectorSettingsUrls.PRIVACY_POLICY)
        )
    }
}
