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

package im.vector.riotx.features.home.room.detail.widget

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.matrix.android.api.session.widgets.model.Widget
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.ClickListener
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.onClick

@EpoxyModelClass(layout = R.layout.item_room_widget)
abstract class RoomWidgetItem : EpoxyModelWithHolder<RoomWidgetItem.Holder>() {

    @EpoxyAttribute lateinit var widget: Widget
    @EpoxyAttribute var widgetClicked: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.widgetName.text = widget.name
        holder.view.onClick(widgetClicked)
    }

    class Holder : VectorEpoxyHolder() {
        val widgetName by bind<TextView>(R.id.roomWidgetName)
    }
}
