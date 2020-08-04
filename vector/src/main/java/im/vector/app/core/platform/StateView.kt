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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import im.vector.app.R
import kotlinx.android.synthetic.main.view_state.view.*

class StateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    sealed class State {
        object Content : State()
        object Loading : State()
        data class Empty(val title: CharSequence? = null, val image: Drawable? = null, val message: CharSequence? = null) : State()

        data class Error(val message: CharSequence? = null) : State()
    }

    var eventCallback: EventCallback? = null

    var contentView: View? = null

    var state: State = State.Empty()
        set(newState) {
            if (newState != state) {
                update(newState)
            }
        }

    interface EventCallback {
        fun onRetryClicked()
    }

    init {
        View.inflate(context, R.layout.view_state, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        errorRetryView.setOnClickListener {
            eventCallback?.onRetryClicked()
        }
        state = State.Content
    }

    private fun update(newState: State) {
        progressBar.isVisible = newState is State.Loading
        errorView.isVisible = newState is State.Error
        emptyView.isVisible = newState is State.Empty
        contentView?.isVisible = newState is State.Content

        when (newState) {
            is State.Content -> Unit
            is State.Loading -> Unit
            is State.Empty   -> {
                emptyImageView.setImageDrawable(newState.image)
                emptyMessageView.text = newState.message
                emptyTitleView.text = newState.title
            }
            is State.Error   -> {
                errorMessageView.text = newState.message
            }
        }
    }
}
