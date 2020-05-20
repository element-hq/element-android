/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.widgets.room

interface WidgetParams {
    val params: Map<String, String>
}

class IntegrationManagerParams(
        private val widgetId: String? = null,
        private val screenId: String? = null) : WidgetParams {

    override val params: Map<String, String> by lazy {
        buildParams()
    }

    private fun buildParams(): Map<String, String> {
        val map = HashMap<String, String>()
        if (widgetId != null) {
            map["integ_id"] = widgetId
        }
        if (screenId != null) {
            map["screen"] = screenId
        }
        return map
    }
}

