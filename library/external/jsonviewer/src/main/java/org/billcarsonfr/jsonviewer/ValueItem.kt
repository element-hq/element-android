/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.billcarsonfr.jsonviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.view.ContextMenu
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

@EpoxyModelClass
internal abstract class ValueItem : EpoxyModelWithHolder<ValueItem.Holder>() {

    @EpoxyAttribute
    var text: EpoxyCharSequence? = null

    @EpoxyAttribute
    var depth: Int = 0

    @EpoxyAttribute
    var copyValue: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickListener: View.OnClickListener? = null

    override fun getDefaultLayout() = R.layout.item_jv_base_value

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.text = text?.charSequence
        holder.baseView.setPadding(Utils.dpToPx(16 * depth, holder.baseView.context), 0, 0, 0)
        itemClickListener?.let { holder.baseView.setOnClickListener(it) }
        holder.copyValue = copyValue
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.baseView.setOnClickListener(null)
        holder.copyValue = null
    }

    class Holder : EpoxyHolder(), View.OnCreateContextMenuListener {

        lateinit var textView: TextView
        lateinit var baseView: LinearLayout
        var copyValue: String? = null

        override fun bindView(itemView: View) {
            baseView = itemView.findViewById(R.id.jvBaseLayout)
            textView = itemView.findViewById(R.id.jvValueText)
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
                menu: ContextMenu?,
                v: View?,
                menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            if (copyValue != null) {
                val menuItem = menu?.add(R.string.copy_value)
                val clipService = v?.context?.getSystemService<ClipboardManager>()
                menuItem?.setOnMenuItemClickListener {
                    clipService?.setPrimaryClip(ClipData.newPlainText("", copyValue))
                    true
                }
            }
        }
    }
}
