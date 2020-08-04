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
package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IntRange
import butterknife.BindColor
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.app.R

/**
 * A password strength bar custom widget
 *  Strength is an Integer
 *  -> 0 No strength
 *  -> 1 Weak
 *  -> 2 Fair
 *  -> 3 Good
 *  -> 4 Strong
 */
class PasswordStrengthBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    @BindView(R.id.password_strength_bar_1)
    lateinit var bar1: View

    @BindView(R.id.password_strength_bar_2)
    lateinit var bar2: View

    @BindView(R.id.password_strength_bar_3)
    lateinit var bar3: View

    @BindView(R.id.password_strength_bar_4)
    lateinit var bar4: View

    @BindColor(R.color.password_strength_bar_undefined)
    @JvmField
    var colorBackground: Int = 0
    @BindColor(R.color.password_strength_bar_weak)
    @JvmField
    var colorWeak: Int = 0
    @BindColor(R.color.password_strength_bar_low)
    @JvmField
    var colorLow: Int = 0
    @BindColor(R.color.password_strength_bar_ok)
    @JvmField
    var colorOk: Int = 0
    @BindColor(R.color.password_strength_bar_strong)
    @JvmField
    var colorStrong: Int = 0

    @IntRange(from = 0, to = 4)
    var strength = 0
        set(newValue) {
            field = newValue.coerceIn(0, 4)

            when (newValue) {
                0 -> {
                    bar1.setBackgroundColor(colorBackground)
                    bar2.setBackgroundColor(colorBackground)
                    bar3.setBackgroundColor(colorBackground)
                    bar4.setBackgroundColor(colorBackground)
                }
                1 -> {
                    bar1.setBackgroundColor(colorWeak)
                    bar2.setBackgroundColor(colorBackground)
                    bar3.setBackgroundColor(colorBackground)
                    bar4.setBackgroundColor(colorBackground)
                }
                2 -> {
                    bar1.setBackgroundColor(colorLow)
                    bar2.setBackgroundColor(colorLow)
                    bar3.setBackgroundColor(colorBackground)
                    bar4.setBackgroundColor(colorBackground)
                }
                3 -> {
                    bar1.setBackgroundColor(colorOk)
                    bar2.setBackgroundColor(colorOk)
                    bar3.setBackgroundColor(colorOk)
                    bar4.setBackgroundColor(colorBackground)
                }
                4 -> {
                    bar1.setBackgroundColor(colorStrong)
                    bar2.setBackgroundColor(colorStrong)
                    bar3.setBackgroundColor(colorStrong)
                    bar4.setBackgroundColor(colorStrong)
                }
            }
        }

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.view_password_strength_bar, this, true)
        orientation = HORIZONTAL
        ButterKnife.bind(this)
        strength = 0
    }
}
