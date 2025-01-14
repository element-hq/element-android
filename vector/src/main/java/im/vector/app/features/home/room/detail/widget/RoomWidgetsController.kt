/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.widget

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.widgets.model.Widget
import javax.inject.Inject

/**
 * Epoxy controller for room widgets list.
 */
class RoomWidgetsController @Inject constructor(
        val stringProvider: StringProvider,
        val colorProvider: ColorProvider
) :
        TypedEpoxyController<List<Widget>>() {

    var listener: Listener? = null

    override fun buildModels(widgets: List<Widget>) {
        val host = this
        if (widgets.isEmpty()) {
            genericFooterItem {
                id("empty")
                text(host.stringProvider.getString(CommonStrings.room_no_active_widgets).toEpoxyCharSequence())
            }
        } else {
            widgets.forEach { widget ->
                roomWidgetItem {
                    id(widget.widgetId)
                    widget(widget)
                    widgetClicked { host.listener?.didSelectWidget(widget) }
                }
            }
        }
        genericButtonItem {
            id("addIntegration")
            text(host.stringProvider.getString(CommonStrings.room_manage_integrations))
            textColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            buttonClickAction { host.listener?.didSelectManageWidgets() }
        }
    }

    interface Listener {
        fun didSelectWidget(widget: Widget)
        fun didSelectManageWidgets()
    }
}
