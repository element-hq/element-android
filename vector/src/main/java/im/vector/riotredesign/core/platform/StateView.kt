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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import im.vector.riotredesign.R
import kotlinx.android.synthetic.main.view_state.view.*

class StateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    sealed class State {
        object Content : State()
        object Loading : State()
        data class Empty(val title: CharSequence? = null, val image: Drawable? = null, val message: CharSequence? = null) : State()
        data class Error(val message: CharSequence? = null) : State()
    }


    private var eventCallback: EventCallback? = null

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
        layoutParams = LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        errorRetryView.setOnClickListener {
            eventCallback?.onRetryClicked()
        }
        state = State.Content
    }


    private fun update(newState: State) {
        when (newState) {
            is State.Content -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.INVISIBLE
                contentView?.visibility = View.VISIBLE
            }
            is State.Loading -> {
                progressBar.visibility = View.VISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.INVISIBLE
                contentView?.visibility = View.INVISIBLE
            }
            is State.Empty   -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.VISIBLE
                emptyImageView.setImageDrawable(newState.image)
                emptyMessageView.text = newState.message
                emptyTitleView.text = newState.title
                if (contentView != null) {
                    contentView!!.visibility = View.INVISIBLE
                }
            }
            is State.Error   -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.VISIBLE
                emptyView.visibility = View.INVISIBLE
                errorMessageView.text = newState.message
                if (contentView != null) {
                    contentView!!.visibility = View.INVISIBLE
                }
            }
        }
    }
}
