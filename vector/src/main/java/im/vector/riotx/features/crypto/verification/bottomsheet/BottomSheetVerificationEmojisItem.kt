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
package im.vector.riotx.features.crypto.verification.bottomsheet

import android.view.ViewGroup
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.api.session.crypto.sas.EmojiRepresentation
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel

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
        holder.emoji0View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation0.emoji
        holder.emoji0View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation0.nameResId)

        holder.emoji1View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation1.emoji
        holder.emoji1View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation1.nameResId)

        holder.emoji2View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation2.emoji
        holder.emoji2View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation2.nameResId)

        holder.emoji3View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation3.emoji
        holder.emoji3View.findViewById<TextView>(R.id.item_emoji_name_tv)?.setText(emojiRepresentation3.nameResId)

        holder.emoji4View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation4.emoji
        holder.emoji4View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation4.nameResId)

        holder.emoji5View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation5.emoji
        holder.emoji5View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation5.nameResId)

        holder.emoji6View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation6.emoji
        holder.emoji6View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation6.nameResId)
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
