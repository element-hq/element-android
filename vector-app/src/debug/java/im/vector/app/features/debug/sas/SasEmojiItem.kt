/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.sas

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.application.R
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation

@EpoxyModelClass
abstract class SasEmojiItem : VectorEpoxyModel<SasEmojiItem.Holder>(R.layout.item_sas_emoji) {

    @EpoxyAttribute
    var index: Int = 0

    @EpoxyAttribute
    lateinit var emojiRepresentation: EmojiRepresentation

    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.indexView.text = "$index:"
        holder.emojiView.text = span {
            +emojiRepresentation.emoji
            emojiRepresentation.drawableRes?.let {
                image(ContextCompat.getDrawable(holder.view.context, it)!!)
            }
        }
        holder.textView.setText(emojiRepresentation.nameResId)
        holder.idView.text = holder.idView.resources.getResourceEntryName(emojiRepresentation.nameResId)
    }

    class Holder : VectorEpoxyHolder() {
        val indexView by bind<TextView>(R.id.sas_emoji_index)
        val emojiView by bind<TextView>(R.id.sas_emoji)
        val textView by bind<TextView>(R.id.sas_emoji_text)
        val idView by bind<TextView>(R.id.sas_emoji_text_id)
    }
}
