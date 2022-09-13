/*
 * Copyright (c) 2020 New Vector Ltd
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

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import im.vector.app.R
import im.vector.app.databinding.ViewJoinConferenceBinding

class JoinConferenceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var views: ViewJoinConferenceBinding? = null
    var onJoinClicked: (() -> Unit)? = null
    var backgroundAnimator: Animator? = null

    init {
        inflate(context, R.layout.view_join_conference, this)
    }

    @SuppressLint("Recycle")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        views = ViewJoinConferenceBinding.bind(this)
        views?.joinConferenceButton?.setOnClickListener { onJoinClicked?.invoke() }
        val colorFrom = ContextCompat.getColor(context, R.color.palette_element_green)
        val colorTo = ContextCompat.getColor(context, R.color.join_conference_animated_color)
        // Animate button color to highlight
        backgroundAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            duration = 500
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                views?.joinConferenceButton?.setBackgroundColor(color)
            }
        }
        backgroundAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        views = null
        backgroundAnimator?.cancel()
    }
}
