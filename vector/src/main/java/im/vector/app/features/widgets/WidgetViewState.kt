/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType

enum class WidgetStatus {
    UNKNOWN,
    WIDGET_NOT_ALLOWED,
    WIDGET_ALLOWED
}

enum class WidgetKind(@StringRes val nameRes: Int, val screenId: String?) {
    ROOM(CommonStrings.room_widget_activity_title, null),
    STICKER_PICKER(CommonStrings.title_activity_choose_sticker, WidgetType.StickerPicker.preferred),
    INTEGRATION_MANAGER(0, null),
    ELEMENT_CALL(0, null);

    fun isAdmin(): Boolean {
        return this == STICKER_PICKER || this == INTEGRATION_MANAGER
    }

    fun supportsPictureInPictureMode(): Boolean {
        return this == ELEMENT_CALL
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
