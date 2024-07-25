/*
 * Copyright 2018 New Vector Ltd
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
