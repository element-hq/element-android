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
 *
 */
package im.vector.app.core.epoxy.bottomsheet

import android.graphics.Typeface
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick

/**
 * A quick reaction list for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_bottom_sheet_quick_reaction)
abstract class BottomSheetQuickReactionsItem : VectorEpoxyModel<BottomSheetQuickReactionsItem.Holder>() {

    @EpoxyAttribute
    lateinit var fontProvider: EmojiCompatFontProvider

    @EpoxyAttribute
    lateinit var texts: List<String>

    @EpoxyAttribute
    lateinit var selecteds: List<Boolean>

    @EpoxyAttribute
    var listener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textViews.forEachIndexed { index, textView ->
            textView.typeface = fontProvider.typeface ?: Typeface.DEFAULT
            textView.text = texts[index]
            textView.alpha = if (selecteds[index]) 0.2f else 1f

            textView.onClick {
                listener?.didSelect(texts[index], !selecteds[index])
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        private val quickReaction0 by bind<TextView>(R.id.quickReaction0)
        private val quickReaction1 by bind<TextView>(R.id.quickReaction1)
        private val quickReaction2 by bind<TextView>(R.id.quickReaction2)
        private val quickReaction3 by bind<TextView>(R.id.quickReaction3)
        private val quickReaction4 by bind<TextView>(R.id.quickReaction4)
        private val quickReaction5 by bind<TextView>(R.id.quickReaction5)
        private val quickReaction6 by bind<TextView>(R.id.quickReaction6)
        private val quickReaction7 by bind<TextView>(R.id.quickReaction7)

        val textViews
            get() = listOf(
                    quickReaction0,
                    quickReaction1,
                    quickReaction2,
                    quickReaction3,
                    quickReaction4,
                    quickReaction5,
                    quickReaction6,
                    quickReaction7
            )
    }

    interface Listener {
        fun didSelect(emoji: String, selected: Boolean)
    }
}
