/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.widget

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.widgets.model.Widget
import java.net.URL

@EpoxyModelClass
abstract class RoomWidgetItem : VectorEpoxyModel<RoomWidgetItem.Holder>(R.layout.item_room_widget) {

    @EpoxyAttribute lateinit var widget: Widget
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var widgetClicked: ClickListener? = null

    @DrawableRes
    @EpoxyAttribute var iconRes: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.widgetName.text = widget.name
        holder.widgetUrl.text = tryOrNull { URL(widget.widgetContent.url) }?.host ?: widget.widgetContent.url
        if (iconRes != null) {
            holder.iconImage.isVisible = true
            holder.iconImage.setImageResource(iconRes!!)
        } else {
            holder.iconImage.isVisible = false
        }
        holder.view.onClick(widgetClicked)
    }

    class Holder : VectorEpoxyHolder() {
        val widgetName by bind<TextView>(R.id.roomWidgetName)
        val widgetUrl by bind<TextView>(R.id.roomWidgetUrl)
        val iconImage by bind<ImageView>(R.id.roomWidgetAvatar)
    }
}
