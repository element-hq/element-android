/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.integrationmanager

import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerConfig
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import javax.inject.Inject

internal class DefaultIntegrationManagerService @Inject constructor(private val integrationManager: IntegrationManager) : IntegrationManagerService {

    override fun addListener(listener: IntegrationManagerService.Listener) {
        integrationManager.addListener(listener)
    }

    override fun removeListener(listener: IntegrationManagerService.Listener) {
        integrationManager.removeListener(listener)
    }

    override fun getOrderedConfigs(): List<IntegrationManagerConfig> {
        return integrationManager.getOrderedConfigs()
    }

    override fun getPreferredConfig(): IntegrationManagerConfig {
        return integrationManager.getPreferredConfig()
    }

    override fun isIntegrationEnabled(): Boolean {
        return integrationManager.isIntegrationEnabled()
    }

    override suspend fun setIntegrationEnabled(enable: Boolean) {
        integrationManager.setIntegrationEnabled(enable)
    }

    override suspend fun setWidgetAllowed(stateEventId: String, allowed: Boolean) {
        integrationManager.setWidgetAllowed(stateEventId, allowed)
    }

    override fun isWidgetAllowed(stateEventId: String): Boolean {
        return integrationManager.isWidgetAllowed(stateEventId)
    }

    override suspend fun setNativeWidgetDomainAllowed(widgetType: String, domain: String, allowed: Boolean) {
        integrationManager.setNativeWidgetDomainAllowed(widgetType, domain, allowed)
    }

    override fun isNativeWidgetDomainAllowed(widgetType: String, domain: String): Boolean {
        return integrationManager.isNativeWidgetDomainAllowed(widgetType, domain)
    }
}
