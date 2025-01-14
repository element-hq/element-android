/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets.permissions

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.api.session.widgets.WidgetService

class WidgetPermissionsHelper(
        private val integrationManagerService: IntegrationManagerService,
        private val widgetService: WidgetService
) {

    suspend fun changePermission(roomId: String, widgetId: String, allow: Boolean) {
        val widget = widgetService.getRoomWidgets(
                roomId = roomId,
                widgetId = QueryStringValue.Equals(widgetId, QueryStringValue.Case.SENSITIVE)
        ).firstOrNull()
        val eventId = widget?.event?.eventId ?: return
        integrationManagerService.setWidgetAllowed(eventId, allow)
    }
}
