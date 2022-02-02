/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.extensions

import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.platform.showOptimizedSnackbar
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.features.themes.ThemeUtils

/**
 * Set a text in the TextView, or set visibility to GONE if the text is null
 */
fun TextView.setTextOrHide(newText: CharSequence?, hideWhenBlank: Boolean = true, vararg relatedViews: View = emptyArray()) {
    if (newText == null ||
            (newText.isBlank() && hideWhenBlank)) {
        isVisible = false
        relatedViews.forEach { it.isVisible = false }
    } else {
        this.text = newText
        isVisible = true
        relatedViews.forEach { it.isVisible = true }
    }
}

/**
 * Set text with a colored part
 * @param fullTextRes the resource id of the full text. Value MUST contains a parameter for string, which will be replaced by the colored part
 * @param coloredTextRes the resource id of the colored part of the text
 * @param colorAttribute attribute of the color. Default to colorPrimary
 * @param underline true to also underline the text. Default to false
 * @param onClick attributes to handle click on the colored part if needed
 */
fun TextView.setTextWithColoredPart(@StringRes fullTextRes: Int,
                                    @StringRes coloredTextRes: Int,
                                    @AttrRes colorAttribute: Int = R.attr.colorPrimary,
                                    underline: Boolean = false,
                                    onClick: (() -> Unit)? = null) {
    val coloredPart = resources.getString(coloredTextRes)
    // Insert colored part into the full text
    val fullText = resources.getString(fullTextRes, coloredPart)

    setTextWithColoredPart(fullText, coloredPart, colorAttribute, underline, onClick)
}

/**
 * Set text with a colored part
 * @param fullText The full text.
 * @param coloredPart The colored part of the text
 * @param colorAttribute attribute of the color. Default to colorPrimary
 * @param underline true to also underline the text. Default to false
 * @param onClick attributes to handle click on the colored part if needed
 */
fun TextView.setTextWithColoredPart(fullText: String,
                                    coloredPart: String,
                                    @AttrRes colorAttribute: Int = R.attr.colorPrimary,
                                    underline: Boolean = true,
                                    onClick: (() -> Unit)? = null) {
    val color = ThemeUtils.getColor(context, colorAttribute)

    val foregroundSpan = ForegroundColorSpan(color)

    val index = fullText.indexOf(coloredPart)

    text = SpannableString(fullText)
            .apply {
                setSpan(foregroundSpan, index, index + coloredPart.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (onClick != null) {
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onClick()
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = color
                        }
                    }
                    setSpan(clickableSpan, index, index + coloredPart.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    movementMethod = LinkMovementMethod.getInstance()
                }
                if (underline) {
                    setSpan(UnderlineSpan(), index, index + coloredPart.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
}

fun TextView.setLeftDrawable(@DrawableRes iconRes: Int, @AttrRes tintColor: Int? = null) {
    val icon = if (tintColor != null) {
        val tint = ThemeUtils.getColor(context, tintColor)
        ContextCompat.getDrawable(context, iconRes)?.also {
            DrawableCompat.setTint(it.mutate(), tint)
        }
    } else {
        ContextCompat.getDrawable(context, iconRes)
    }
    setLeftDrawable(icon)
}

fun TextView.setLeftDrawable(drawable: Drawable?) {
    setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
}

fun TextView.clearDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
}

/**
 * Set long click listener to copy the current text of the TextView to the clipboard and show a Snackbar
 */
fun TextView.copyOnLongClick() {
    setOnLongClickListener { view ->
        (view as? TextView)
                ?.text
                ?.let { text ->
                    copyToClipboard(view.context, text, false)
                    view.showOptimizedSnackbar(view.resources.getString(R.string.copied_to_clipboard))
                }
        true
    }
}
