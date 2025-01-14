/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
