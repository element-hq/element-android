/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewStub
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.Guideline
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.platform.CheckableView
import im.vector.riotx.core.ui.views.ReadMarkerView
import im.vector.riotx.core.ui.views.ReadReceiptsView
import im.vector.riotx.core.utils.DimensionUtils.dpToPx
import org.w3c.dom.Attr

/**
 * Children must override getViewType()
 */
abstract class BaseEventItem<H : BaseEventItem.BaseHolder> : VectorEpoxyModel<H>() {

    // To use for instance when opening a permalink with an eventId
    @EpoxyAttribute
    var highlighted: Boolean = false
    @EpoxyAttribute
    open var leftGuideline: Int = 0

    override fun bind(holder: H) {
        super.bind(holder)
        holder.leftGuideline.setGuidelineBegin(leftGuideline)
        holder.checkableBackground.isChecked = highlighted
    }

    abstract class BaseHolder(@IdRes val stubId: Int) : VectorEpoxyHolder() {
        val leftGuideline by bind<Guideline>(R.id.messageStartGuideline)
        val checkableBackground by bind<CheckableView>(R.id.messageSelectedBackground)
        val readReceiptsView by bind<ReadReceiptsView>(R.id.readReceiptsView)
        val readMarkerView by bind<ReadMarkerView>(R.id.readMarkerView)

        override fun bindView(itemView: View) {
            super.bindView(itemView)
            inflateStub()
        }

        private fun inflateStub() {
            view.findViewById<ViewStub>(stubId).inflate()
        }
    }

}