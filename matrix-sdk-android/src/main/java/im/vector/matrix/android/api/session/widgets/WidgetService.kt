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

package im.vector.matrix.android.api.session.widgets

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.session.widgets.Widget

interface WidgetService {

    fun getWidgetURLFormatter(): WidgetURLFormatter

    fun getWidgetPostAPIMediator(): WidgetPostAPIMediator

    fun getRoomWidgets(
            roomId: String,
            widgetId: QueryStringValue = QueryStringValue.NoCondition,
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): List<Widget>

    fun getRoomWidgetsLive(
            roomId: String,
            widgetId: QueryStringValue = QueryStringValue.NoCondition,
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): LiveData<List<Widget>>

    fun getUserWidgets(
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): List<Widget>

    fun getUserWidgetsLive(
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): LiveData<List<Widget>>

    fun createRoomWidget(roomId: String, widgetId: String, content: Content, callback: MatrixCallback<Widget>): Cancelable

    fun destroyRoomWidget(roomId: String, widgetId: String, callback: MatrixCallback<Unit>): Cancelable

    fun hasPermissionsToHandleWidgets(roomId: String): Boolean
}
