/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.details

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.utils.DimensionConverter

private const val EXTRA_TOP_MARGIN_DP = 32

@EpoxyModelClass
abstract class SessionDetailsHeaderItem : VectorEpoxyModel<SessionDetailsHeaderItem.Holder>(R.layout.item_session_details_header) {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var addExtraTopMargin: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var dimensionConverter: DimensionConverter? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.sessionDetailsHeaderTitle.text = title
        val topMargin = if (addExtraTopMargin) {
            dimensionConverter?.dpToPx(EXTRA_TOP_MARGIN_DP) ?: 0
        } else {
            0
        }
        holder.sessionDetailsHeaderTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(top = topMargin)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val sessionDetailsHeaderTitle by bind<TextView>(R.id.sessionDetailsHeaderTitle)
    }
}
