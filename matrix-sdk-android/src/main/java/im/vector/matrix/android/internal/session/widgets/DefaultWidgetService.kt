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

package im.vector.matrix.android.internal.session.widgets

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.widgets.WidgetPostAPIMediator
import im.vector.matrix.android.api.session.widgets.WidgetService
import im.vector.matrix.android.api.session.widgets.WidgetURLFormatter
import im.vector.matrix.android.api.session.widgets.model.Widget
import im.vector.matrix.android.api.util.Cancelable
import javax.inject.Inject
import javax.inject.Provider

internal class DefaultWidgetService @Inject constructor(private val widgetManager: WidgetManager,
                                                        private val widgetURLFormatter: WidgetURLFormatter,
                                                        private val widgetPostAPIMediator: Provider<WidgetPostAPIMediator>)
    : WidgetService {

    override fun getWidgetURLFormatter(): WidgetURLFormatter {
        return widgetURLFormatter
    }

    override fun getWidgetPostAPIMediator(): WidgetPostAPIMediator {
        return widgetPostAPIMediator.get()
    }

    override fun getRoomWidgets(
            roomId: String,
            widgetId: QueryStringValue,
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): List<Widget> {
        return widgetManager.getRoomWidgets(roomId, widgetId, widgetTypes, excludedTypes)
    }

    override fun getRoomWidgetsLive(
            roomId: String,
            widgetId: QueryStringValue,
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): LiveData<List<Widget>> {
        return widgetManager.getRoomWidgetsLive(roomId, widgetId, widgetTypes, excludedTypes)
    }

    override fun getUserWidgetsLive(
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): LiveData<List<Widget>> {
        return widgetManager.getUserWidgetsLive(widgetTypes, excludedTypes)
    }

    override fun getUserWidgets(
            widgetTypes: Set<String>?,
            excludedTypes: Set<String>?
    ): List<Widget> {
        return widgetManager.getUserWidgets(widgetTypes, excludedTypes)
    }

    override fun createRoomWidget(
            roomId: String,
            widgetId: String,
            content: Content,
            callback: MatrixCallback<Widget>
    ): Cancelable {
        return widgetManager.createRoomWidget(roomId, widgetId, content, callback)
    }

    override fun destroyRoomWidget(
            roomId: String,
            widgetId: String,
            callback: MatrixCallback<Unit>
    ): Cancelable {
        return widgetManager.destroyRoomWidget(roomId, widgetId, callback)
    }

    override fun hasPermissionsToHandleWidgets(roomId: String): Boolean {
        return widgetManager.hasPermissionsToHandleWidgets(roomId)
    }
}
