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

package im.vector.app.features.home.room.detail.widget

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.widgets.model.Widget
import javax.inject.Inject

/**
 * Epoxy controller for room widgets list
 */
class RoomWidgetController @Inject constructor() : TypedEpoxyController<List<Widget>>() {

    var listener: Listener? = null

    override fun buildModels(widget: List<Widget>) {
        widget.forEach {
            RoomWidgetItem_()
                    .id(it.widgetId)
                    .widget(it)
                    .widgetClicked { listener?.didSelectWidget(it) }
                    .addTo(this)
        }
    }

    interface Listener {
        fun didSelectWidget(widget: Widget)
    }
}
