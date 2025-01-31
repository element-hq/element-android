/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.graphics.drawable.Drawable
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.features.themes.ThemeUtils

/**
 * Remove left margin of a SearchView.
 */
fun SearchView.withoutLeftMargin() {
    (findViewById<View>(R.id.search_edit_frame))?.let {
        val searchEditFrameParams = it.layoutParams as ViewGroup.MarginLayoutParams
        searchEditFrameParams.leftMargin = 0
        it.layoutParams = searchEditFrameParams
    }

    (findViewById<View>(R.id.search_mag_icon))?.let {
        val searchIconParams = it.layoutParams as ViewGroup.MarginLayoutParams
        searchIconParams.leftMargin = 0
        it.layoutParams = searchIconParams
    }
}

fun EditText.hidePassword() {
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
}

fun View.getMeasurements(): Pair<Int, Int> {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val width = measuredWidth
    val height = measuredHeight
    return width to height
}

fun ImageView.setDrawableOrHide(drawableRes: Drawable?) {
    setImageDrawable(drawableRes)
    isVisible = drawableRes != null
}

fun View.setAttributeTintedBackground(@DrawableRes drawableRes: Int, @AttrRes tint: Int) {
    val drawable = ContextCompat.getDrawable(context, drawableRes)!!
    DrawableCompat.setTint(drawable, ThemeUtils.getColor(context, tint))
    background = drawable
}

fun View.tintBackground(@ColorInt tintColor: Int) {
    val bkg = background?.let {
        val backgroundDrawable = DrawableCompat.wrap(background)
        DrawableCompat.setTint(backgroundDrawable, tintColor)
        backgroundDrawable
    }
    background = bkg
}

fun ImageView.setAttributeTintedImageResource(@DrawableRes drawableRes: Int, @AttrRes tint: Int) {
    val drawable = ContextCompat.getDrawable(context, drawableRes)!!
    DrawableCompat.setTint(drawable, ThemeUtils.getColor(context, tint))
    setImageDrawable(drawable)
}

fun View.setAttributeBackground(@AttrRes attributeId: Int) {
    val attribute = ThemeUtils.getAttribute(context, attributeId)!!
    setBackgroundResource(attribute.resourceId)
}
