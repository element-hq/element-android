/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.autocomplete.emoji

import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.reactions.data.EmojiItem
import javax.inject.Inject

class AutocompleteEmojiController @Inject constructor(
        private val fontProvider: EmojiCompatFontProvider
) : TypedEpoxyController<List<EmojiItem>>() {

    var emojiTypeface: Typeface? = fontProvider.typeface

    private val fontProviderListener = object : EmojiCompatFontProvider.FontProviderListener {
        override fun compatibilityFontUpdate(typeface: Typeface?) {
            emojiTypeface = typeface
        }
    }

    var listener: AutocompleteClickListener<String>? = null

    override fun buildModels(data: List<EmojiItem>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        val host = this
        data
                .take(MAX)
                .forEach { emojiItem ->
                    autocompleteEmojiItem {
                        id(emojiItem.name)
                        emojiItem(emojiItem)
                        emojiTypeFace(host.emojiTypeface)
                        onClickListener { host.listener?.onItemClick(emojiItem.emoji) }
                    }
                }

        if (data.size > MAX) {
            autocompleteMoreResultItem {
                id("more_result")
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        fontProvider.addListener(fontProviderListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        fontProvider.removeListener(fontProviderListener)
    }

    companion object {
        const val MAX = 50
    }
}
