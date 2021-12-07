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

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user clicks/taps on a UI element.
 */
data class Click(
    /**
     * The index of the element, if its in a list of elements.
     */
    val index: Int? = null,
    /**
     * The unique name of this element.
     */
    val name: Name,
) : VectorAnalyticsEvent {

    enum class Name {
        SendMessageButton,
    }

    override fun getName() = "Click"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            index?.let { put("index", it) }
            put("name", name.name)
        }.takeIf { it.isNotEmpty() }
    }
}
