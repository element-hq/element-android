/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.analytics.experiment

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

data class ExperimentInteraction(
        /**
         * The unique name of this element.
         */
        val name: Name,

        val extra: Map<String, Any> = emptyMap()
) : VectorAnalyticsEvent {

    enum class Name {
        SpaceSwitchHeader,
        SpaceSwitchHeaderAdd,
        SpaceSwitchHeaderCreate,
    }

    override fun getName() = "Interaction"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("name", name.name)
            putAll(extra)
        }.takeIf { it.isNotEmpty() }
    }
}
