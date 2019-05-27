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

package im.vector.riotredesign.core.platform

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import im.vector.riotredesign.R
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

    init {
        View.inflate(context, R.layout.view_button_state, this)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        buttonStateButton.setOnClickListener {
            callback?.onButtonClicked()
        }

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
                        buttonStateButton.text = getString(R.styleable.ButtonStateView_bsv_button_text)
                        buttonStateLoaded.setImageDrawable(getDrawable(R.styleable.ButtonStateView_bsv_loaded_image_src))
                    } finally {
                        recycle()
                    }
                }
    }

    fun render(newState: State) {
        if (newState == State.Button) {
            buttonStateButton.isVisible = true
        } else {
            // We use isInvisible because we want to keep button space in the layout
            buttonStateButton.isInvisible = true
        }

        buttonStateLoading.isVisible = newState == State.Loading
        buttonStateLoaded.isVisible = newState == State.Loaded
        buttonStateRetry.isVisible = newState == State.Error
    }
}
