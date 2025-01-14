/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import im.vector.app.R
import im.vector.app.databinding.ViewRoomWidgetsBannerBinding
import im.vector.lib.strings.CommonPlurals
import org.matrix.android.sdk.api.session.widgets.model.Widget

class RoomWidgetsBannerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onViewWidgetsClicked()
    }

    private val views: ViewRoomWidgetsBannerBinding

    var callback: Callback? = null

    init {
        setupView()
        views = ViewRoomWidgetsBannerBinding.bind(this)
    }

    private fun setupView() {
        inflate(context, R.layout.view_room_widgets_banner, this)
        setBackgroundResource(R.drawable.bg_active_widgets_banner)
        setOnClickListener {
            callback?.onViewWidgetsClicked()
        }
    }

    fun render(widgets: List<Widget>?) {
        if (widgets.isNullOrEmpty()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            views.activeWidgetsLabel.text = context.resources.getQuantityString(CommonPlurals.active_widgets, widgets.size, widgets.size)
        }
    }
}
