/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.sticker

import im.vector.app.features.home.room.detail.RoomDetailViewEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import javax.inject.Inject

class StickerPickerActionHandler @Inject constructor(private val session: Session) {

    suspend fun handle(): RoomDetailViewEvents = withContext(Dispatchers.Default) {
        // Search for the sticker picker widget in the user account
        val integrationsEnabled = session.integrationManagerService().isIntegrationEnabled()
        if (!integrationsEnabled) {
            return@withContext RoomDetailViewEvents.DisplayEnableIntegrationsWarning
        }
        val stickerWidget = session.widgetService().getUserWidgets(WidgetType.StickerPicker.values()).firstOrNull { it.isActive }
        if (stickerWidget == null || stickerWidget.widgetContent.url.isNullOrBlank()) {
            RoomDetailViewEvents.DisplayPromptForIntegrationManager
        } else {
            RoomDetailViewEvents.OpenStickerPicker(
                    widget = stickerWidget
            )
        }
    }
}
