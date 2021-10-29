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

package im.vector.app.features.widgets

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType

enum class WidgetStatus {
    UNKNOWN,
    WIDGET_NOT_ALLOWED,
    WIDGET_ALLOWED
}

enum class WidgetKind(@StringRes val nameRes: Int, val screenId: String?) {
    ROOM(R.string.room_widget_activity_title, null),
    STICKER_PICKER(R.string.title_activity_choose_sticker, WidgetType.StickerPicker.preferred),
    INTEGRATION_MANAGER(0, null);

    fun isAdmin(): Boolean {
        return this == STICKER_PICKER || this == INTEGRATION_MANAGER
    }
}

data class WidgetViewState(
        val roomId: String,
        val baseUrl: String,
        val urlParams: Map<String, String> = emptyMap(),
        val widgetId: String? = null,
        val widgetKind: WidgetKind,
        val status: WidgetStatus = WidgetStatus.UNKNOWN,
        val formattedURL: Async<String> = Uninitialized,
        val webviewLoadedUrl: Async<String> = Uninitialized,
        val widgetName: String = "",
        val canManageWidgets: Boolean = false,
        val asyncWidget: Async<Widget> = Uninitialized
) : MavericksState {

    constructor(widgetArgs: WidgetArgs) : this(
            widgetKind = widgetArgs.kind,
            baseUrl = widgetArgs.baseUrl,
            roomId = widgetArgs.roomId,
            widgetId = widgetArgs.widgetId,
            urlParams = widgetArgs.urlParams
    )
}
