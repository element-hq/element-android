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

package im.vector.riotx.features.widgets

import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.widgets.room.WidgetArgs
import im.vector.riotx.features.widgets.room.WidgetKind
import javax.inject.Inject

class WidgetArgsBuilder @Inject constructor(private val sessionHolder: ActiveSessionHolder) {

    @Suppress("UNCHECKED_CAST")
    fun buildIntegrationManagerArgs(roomId: String, integId: String?, screenId: String?): WidgetArgs {
        val session = sessionHolder.getActiveSession()
        val integrationManagerConfig = session.integrationManagerService().getPreferredConfig()
        return WidgetArgs(
                baseUrl = integrationManagerConfig.uiUrl,
                kind = WidgetKind.INTEGRATION_MANAGER,
                roomId = roomId,
                urlParams = mapOf(
                        "screen" to screenId,
                        "integ_id" to integId,
                        "room_id" to roomId
                ).filterValues { it != null } as Map<String, String>
        )
    }
}
