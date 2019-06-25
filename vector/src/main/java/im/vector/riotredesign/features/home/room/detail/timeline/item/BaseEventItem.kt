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
package im.vector.riotredesign.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewStub
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.Guideline
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.platform.CheckableView
import im.vector.riotredesign.core.utils.DimensionUtils.dpToPx

abstract class BaseEventItem<H : BaseEventItem.BaseHolder> : VectorEpoxyModel<H>() {

    var avatarStyle: AvatarStyle = AvatarStyle.SMALL

    // To use for instance when opening a permalink with an eventId
    @EpoxyAttribute
    var highlighted: Boolean = false

    override fun bind(holder: H) {
        super.bind(holder)
        //optimize?
        val px = dpToPx(avatarStyle.avatarSizeDP + 8, holder.view.context)
        holder.leftGuideline.setGuidelineBegin(px)

        holder.checkableBackground.isChecked = highlighted
    }


    override fun getViewType(): Int {
        return getStubType()
    }

    abstract fun getStubType(): Int


    abstract class BaseHolder : VectorEpoxyHolder() {

        val leftGuideline by bind<Guideline>(R.id.messageStartGuideline)
        val checkableBackground by bind<CheckableView>(R.id.messageSelectedBackground)

        @IdRes
        abstract fun getStubId(): Int

        override fun bindView(itemView: View) {
            super.bindView(itemView)
            inflateStub()
        }

        private fun inflateStub() {
            view.findViewById<ViewStub>(getStubId()).inflate()
        }

    }

    companion object {

        enum class AvatarStyle(val avatarSizeDP: Int) {
            BIG(50),
            MEDIUM(40),
            SMALL(30),
            NONE(0)
        }
    }
}