/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import im.vector.app.databinding.ViewPasswordStrengthBarBinding
import im.vector.lib.ui.styles.R

/**
 * A password strength bar custom widget.
 * Strength is an Integer
 *  -> 0 No strength
 *  -> 1 Weak
 *  -> 2 Fair
 *  -> 3 Good
 *  -> 4 Strong
 */
class PasswordStrengthBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) :
        LinearLayout(context, attrs, defStyleAttr) {

    private val views: ViewPasswordStrengthBarBinding

    private val colorBackground = ContextCompat.getColor(context, R.color.password_strength_bar_undefined)
    private val colorWeak = ContextCompat.getColor(context, R.color.password_strength_bar_weak)
    private val colorLow = ContextCompat.getColor(context, R.color.password_strength_bar_low)
    private val colorOk = ContextCompat.getColor(context, R.color.password_strength_bar_ok)
    private val colorStrong = ContextCompat.getColor(context, R.color.password_strength_bar_strong)

    @IntRange(from = 0, to = 4)
    var strength = 0
        set(newValue) {
            field = newValue.coerceIn(0, 4)

            when (newValue) {
                0 -> {
                    views.passwordStrengthBar1.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar2.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar3.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar4.setBackgroundColor(colorBackground)
                }
                1 -> {
                    views.passwordStrengthBar1.setBackgroundColor(colorWeak)
                    views.passwordStrengthBar2.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar3.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar4.setBackgroundColor(colorBackground)
                }
                2 -> {
                    views.passwordStrengthBar1.setBackgroundColor(colorLow)
                    views.passwordStrengthBar2.setBackgroundColor(colorLow)
                    views.passwordStrengthBar3.setBackgroundColor(colorBackground)
                    views.passwordStrengthBar4.setBackgroundColor(colorBackground)
                }
                3 -> {
                    views.passwordStrengthBar1.setBackgroundColor(colorOk)
                    views.passwordStrengthBar2.setBackgroundColor(colorOk)
                    views.passwordStrengthBar3.setBackgroundColor(colorOk)
                    views.passwordStrengthBar4.setBackgroundColor(colorBackground)
                }
                4 -> {
                    views.passwordStrengthBar1.setBackgroundColor(colorStrong)
                    views.passwordStrengthBar2.setBackgroundColor(colorStrong)
                    views.passwordStrengthBar3.setBackgroundColor(colorStrong)
                    views.passwordStrengthBar4.setBackgroundColor(colorStrong)
                }
            }
        }

    init {
        inflate(context, im.vector.app.R.layout.view_password_strength_bar, this)
        views = ViewPasswordStrengthBarBinding.bind(this)
        orientation = HORIZONTAL
        strength = 0
    }
}
