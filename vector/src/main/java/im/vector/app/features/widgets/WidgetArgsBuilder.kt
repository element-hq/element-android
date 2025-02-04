/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.themes.ThemeProvider
import org.matrix.android.sdk.api.session.widgets.model.Widget
import javax.inject.Inject

class WidgetArgsBuilder @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val themeProvider: ThemeProvider
) {

    @Suppress("UNCHECKED_CAST")
    fun buildIntegrationManagerArgs(roomId: String, integId: String?, screen: String?): WidgetArgs {
        val session = sessionHolder.getActiveSession()
        val integrationManagerConfig = session.integrationManagerService().getPreferredConfig()
        val normalizedScreen = when {
            screen == null -> null
            screen.startsWith("type_") -> screen
            else -> "type_$screen"
        }
        return WidgetArgs(
                baseUrl = integrationManagerConfig.uiUrl,
                kind = WidgetKind.INTEGRATION_MANAGER,
                roomId = roomId,
                urlParams = mapOf(
                        "screen" to normalizedScreen,
                        "integ_id" to integId,
                        "room_id" to roomId,
                        "theme" to getTheme()
                ).filterNotNull()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun buildStickerPickerArgs(roomId: String, widget: Widget): WidgetArgs {
        val widgetId = widget.widgetId
        val baseUrl = sessionHolder.getActiveSession().widgetService()
                .getWidgetComputedUrl(widget, themeProvider.isLightTheme()) ?: throw IllegalStateException()
        return WidgetArgs(
                baseUrl = baseUrl,
                kind = WidgetKind.STICKER_PICKER,
                roomId = roomId,
                widgetId = widgetId,
                urlParams = mapOf(
                        "widgetId" to widgetId,
                        "room_id" to roomId,
                        "theme" to getTheme()
                ).filterNotNull()
        )
    }

    fun buildRoomWidgetArgs(roomId: String, widget: Widget): WidgetArgs {
        val widgetId = widget.widgetId
        val baseUrl = sessionHolder.getActiveSession().widgetService()
                .getWidgetComputedUrl(widget, themeProvider.isLightTheme()) ?: throw IllegalStateException()
        return WidgetArgs(
                baseUrl = baseUrl,
                kind = WidgetKind.ROOM,
                roomId = roomId,
                widgetId = widgetId
        )
    }

    fun buildElementCallWidgetArgs(roomId: String, widget: Widget): WidgetArgs {
        return buildRoomWidgetArgs(roomId, widget)
                .copy(
                        kind = WidgetKind.ELEMENT_CALL
                )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, String?>.filterNotNull(): Map<String, String> {
        return filterValues { it != null } as Map<String, String>
    }

    private fun getTheme(): String {
        return if (themeProvider.isLightTheme()) {
            "light"
        } else {
            "dark"
        }
    }
}
