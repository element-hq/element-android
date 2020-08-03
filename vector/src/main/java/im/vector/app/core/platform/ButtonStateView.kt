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
 */

package im.vector.app.core.platform

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import im.vector.app.R
import kotlinx.android.synthetic.main.view_button_state.view.*

class ButtonStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    sealed class State {
        object Button : State()
        object Loading : State()
        object Loaded : State()
        object Error : State()
    }

    var callback: Callback? = null

    interface Callback {
        fun onButtonClicked()
        fun onRetryClicked()
    }

    // Big or Flat button
    var button: Button

    init {
        View.inflate(context, R.layout.view_button_state, this)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        buttonStateRetry.setOnClickListener {
            callback?.onRetryClicked()
        }

        // Read attributes
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ButtonStateView,
                0, 0)
                .apply {
                    try {
                        if (getBoolean(R.styleable.ButtonStateView_bsv_use_flat_button, true)) {
                            button = buttonStateButtonFlat
                            buttonStateButtonBig.isVisible = false
                        } else {
                            button = buttonStateButtonBig
                            buttonStateButtonFlat.isVisible = false
                        }

                        button.text = getString(R.styleable.ButtonStateView_bsv_button_text)
                        buttonStateLoaded.setImageDrawable(getDrawable(R.styleable.ButtonStateView_bsv_loaded_image_src))
                    } finally {
                        recycle()
                    }
                }

        button.setOnClickListener {
            callback?.onButtonClicked()
        }
    }

    fun render(newState: State) {
        if (newState == State.Button) {
            button.isVisible = true
        } else {
            // We use isInvisible because we want to keep button space in the layout
            button.isInvisible = true
        }

        buttonStateLoading.isVisible = newState == State.Loading
        buttonStateLoaded.isVisible = newState == State.Loaded
        buttonStateRetry.isVisible = newState == State.Error
    }
}
