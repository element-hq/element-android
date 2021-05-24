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
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.databinding.MediaViewStateBinding

class TimelineMediaStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    sealed class State {
        object NotDownloaded : State()
        data class Downloading(val progress: Int, val indeterminate: Boolean) : State()
        object PermanentError : State()
        object ReadyToPlay : State()
        object None : State()
    }

    var callback: Callback? = null

    interface Callback {
        fun onButtonClicked()
    }

    private val views: MediaViewStateBinding

    init {
        inflate(context, R.layout.media_view_state, this)
        views = MediaViewStateBinding.bind(this)

        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

//        views.buttonStateRetry.setOnClickListener {
//            callback?.onRetryClicked()
//        }

        // Read attributes
//        context.theme.obtainStyledAttributes(
//                attrs,
//                R.styleable.ButtonStateView,
//                0, 0)
//                .apply {
//                    try {
//                        if (getBoolean(R.styleable.ButtonStateView_bsv_use_flat_button, true)) {
//                            button = views.buttonStateButtonFlat
//                            views.buttonStateButtonBig.isVisible = false
//                        } else {
//                            button = views.buttonStateButtonBig
//                            views.buttonStateButtonFlat.isVisible = false
//                        }
//
//                        button.text = getString(R.styleable.ButtonStateView_bsv_button_text)
//                        views.buttonStateLoaded.setImageDrawable(getDrawable(R.styleable.ButtonStateView_bsv_loaded_image_src))
//                    } finally {
//                        recycle()
//                    }
//                }

        if (isInEditMode) {
            render(State.ReadyToPlay)
        }
        setOnClickListener(DebouncedClickListener({
            callback?.onButtonClicked()
        }))
    }

    fun render(newState: State) {
        isVisible = newState != State.None
        views.mediaStateNotDownloaded.isVisible = newState == State.NotDownloaded
        views.mediaDownloadProgressBar.isVisible = newState is State.Downloading
        (newState as? State.Downloading)?.let {
            views.mediaDownloadProgressBar.progress = it.progress
            views.mediaDownloadProgressBar.isIndeterminate = it.indeterminate
        }
        views.mediaStateError.isVisible = newState == State.PermanentError
        views.mediaStatePlay.isVisible = newState == State.ReadyToPlay
    }
}
