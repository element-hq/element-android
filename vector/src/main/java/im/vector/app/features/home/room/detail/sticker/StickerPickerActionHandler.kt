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
