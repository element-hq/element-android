/*
 * Copyright 2020 New Vector Ltd
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
 *
 */
package im.vector.app.features.crypto.verification.epoxy

import android.content.Context
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.databinding.ItemEmojiVerifBinding
import me.gujun.android.span.Span
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation

/**
 * A emoji list for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_emojis)
abstract class BottomSheetVerificationEmojisItem : VectorEpoxyModel<BottomSheetVerificationEmojisItem.Holder>() {

    @EpoxyAttribute lateinit var emojiRepresentation0: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation1: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation2: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation3: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation4: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation5: EmojiRepresentation
    @EpoxyAttribute lateinit var emojiRepresentation6: EmojiRepresentation

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindEmojiView(holder.emoji0View, emojiRepresentation0)
        bindEmojiView(holder.emoji1View, emojiRepresentation1)
        bindEmojiView(holder.emoji2View, emojiRepresentation2)
        bindEmojiView(holder.emoji3View, emojiRepresentation3)
        bindEmojiView(holder.emoji4View, emojiRepresentation4)
        bindEmojiView(holder.emoji5View, emojiRepresentation5)
        bindEmojiView(holder.emoji6View, emojiRepresentation6)
    }

    private fun spanForRepresentation(context: Context, rep: EmojiRepresentation): Span {
        return span {
            if (rep.drawableRes != null) {
                ContextCompat.getDrawable(context, rep.drawableRes!!)?.let { image(it) }
            } else {
                +rep.emoji
            }
        }
    }

    private fun bindEmojiView(view: ViewGroup, rep: EmojiRepresentation) {
        val views = ItemEmojiVerifBinding.bind(view)
        val drawableRes = rep.drawableRes
        if (drawableRes != null) {
            views.itemEmojiTv.isVisible = false
            views.itemEmojiImage.isVisible = true
            views.itemEmojiImage.setImageDrawable(ContextCompat.getDrawable(view.context, drawableRes))
        } else {
            views.itemEmojiTv.isVisible = true
            views.itemEmojiImage.isVisible = false
            views.itemEmojiTv.text = rep.emoji
        }
        views.itemEmojiNameTv.setText(rep.nameResId)
    }

    class Holder : VectorEpoxyHolder() {
        val emoji0View by bind<ViewGroup>(R.id.emoji0)
        val emoji1View by bind<ViewGroup>(R.id.emoji1)
        val emoji2View by bind<ViewGroup>(R.id.emoji2)
        val emoji3View by bind<ViewGroup>(R.id.emoji3)
        val emoji4View by bind<ViewGroup>(R.id.emoji4)
        val emoji5View by bind<ViewGroup>(R.id.emoji5)
        val emoji6View by bind<ViewGroup>(R.id.emoji6)
    }
}
