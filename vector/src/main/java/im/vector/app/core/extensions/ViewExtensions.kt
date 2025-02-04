/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.graphics.drawable.Drawable
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import im.vector.app.core.animations.SimpleTransitionListener
import im.vector.app.features.themes.ThemeUtils

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

/**
 * Inspired from https://stackoverflow.com/a/64597532/1472514. Safer to call the 2 available API.
 */
fun View.giveAccessibilityFocus() {
    performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
}

fun ViewGroup.animateLayoutChange(animationDuration: Long, transitionComplete: (() -> Unit)? = null) {
    val transition = TransitionSet().apply {
        ordering = TransitionSet.ORDERING_SEQUENTIAL
        addTransition(ChangeBounds())
        addTransition(Fade(Fade.IN))
        duration = animationDuration
        addListener(object : SimpleTransitionListener() {
            override fun onTransitionEnd(transition: Transition) {
                transitionComplete?.invoke()
            }
        })
    }
    TransitionManager.beginDelayedTransition((parent as? ViewGroup ?: this), transition)
}
