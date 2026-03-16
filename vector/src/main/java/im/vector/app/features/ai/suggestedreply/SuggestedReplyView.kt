/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.suggestedreply

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip

class SuggestedReplyView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val chipContainer: LinearLayout
    private val progressBar: ProgressBar
    private var onSuggestionClick: ((String) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        val padding = (8 * context.resources.displayMetrics.density).toInt()

        val outerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padding, 0, padding, 0)
        }

        progressBar = ProgressBar(context).apply {
            isVisible = false
            isIndeterminate = true
            val size = (24 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = padding
            }
        }
        outerLayout.addView(progressBar)

        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        outerLayout.addView(chipContainer)

        addView(outerLayout)
    }

    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClick = listener
    }

    fun setSuggestions(suggestions: List<String>) {
        chipContainer.removeAllViews()
        progressBar.isVisible = false
        val padding = (4 * context.resources.displayMetrics.density).toInt()
        suggestions.forEach { suggestion ->
            val chip = Chip(context).apply {
                text = suggestion
                isCheckable = false
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = padding
                }
                setOnClickListener { onSuggestionClick?.invoke(suggestion) }
            }
            chipContainer.addView(chip)
        }
        isVisible = suggestions.isNotEmpty()
    }

    fun showLoading() {
        chipContainer.removeAllViews()
        progressBar.isVisible = true
        isVisible = true
    }

    fun clear() {
        chipContainer.removeAllViews()
        progressBar.isVisible = false
        isVisible = false
    }
}
