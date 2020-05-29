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

package im.vector.matrix.android.api.session.integrationmanager

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable

interface IntegrationManagerService {

    interface Listener {
        fun onIsEnabledChanged(enabled: Boolean) {
            // No-op
        }

        fun onConfigurationChanged(configs: List<IntegrationManagerConfig>) {
            // No-op
        }

        fun onWidgetPermissionsChanged(widgets: Map<String, Boolean>) {
            // No-op
        }
    }

    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    fun getOrderedConfigs(): List<IntegrationManagerConfig>

    fun getPreferredConfig(): IntegrationManagerConfig

    fun isIntegrationEnabled(): Boolean

    fun setIntegrationEnabled(enable: Boolean, callback: MatrixCallback<Unit>): Cancelable

    fun setWidgetAllowed(stateEventId: String, allowed: Boolean, callback: MatrixCallback<Unit>): Cancelable

    fun isWidgetAllowed(stateEventId: String): Boolean

    fun setNativeWidgetDomainAllowed(widgetType: String, domain: String, allowed: Boolean, callback: MatrixCallback<Unit>): Cancelable

    fun isNativeWidgetAllowed(widgetType: String, domain: String?): Boolean
}
