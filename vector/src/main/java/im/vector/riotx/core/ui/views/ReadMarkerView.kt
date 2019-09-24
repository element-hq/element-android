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

package im.vector.riotx.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isInvisible
import im.vector.riotx.R
import kotlinx.coroutines.*
import timber.log.Timber

private const val DELAY_IN_MS = 1_500L

class ReadMarkerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Callback {
        fun onReadMarkerLongBound(isDisplayed: Boolean)
    }

    private var eventId: String? = null
    private var callback: Callback? = null
    private var callbackDispatcherJob: Job? = null

    fun bindView(eventId: String?, hasReadMarker: Boolean, displayReadMarker: Boolean, readMarkerCallback: Callback) {
        Timber.v("Bind event $eventId - hasReadMarker: $hasReadMarker - displayReadMarker: $displayReadMarker")
        this.eventId = eventId
        this.callback = readMarkerCallback
        if (displayReadMarker) {
            startAnimation()
        } else {
            this.animation?.cancel()
            this.visibility = INVISIBLE
        }
        if (hasReadMarker) {
            callbackDispatcherJob = GlobalScope.launch(Dispatchers.Main) {
                delay(DELAY_IN_MS)
                callback?.onReadMarkerLongBound(displayReadMarker)
            }
        }
    }

    fun unbind() {
        this.callbackDispatcherJob?.cancel()
        this.callback = null
        this.eventId = null
        this.animation?.cancel()
        this.visibility = INVISIBLE
    }

    private fun startAnimation() {
        if (animation == null) {
            animation = AnimationUtils.loadAnimation(context, R.anim.unread_marker_anim)
            animation.startOffset = DELAY_IN_MS / 2
            animation.duration = DELAY_IN_MS / 2
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                    visibility = INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
        visibility = VISIBLE
        animation.start()
    }

}
