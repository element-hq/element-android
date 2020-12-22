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

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import im.vector.app.R
import im.vector.app.databinding.ViewCurrentCallsBinding
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.call.CallState

class CurrentCallsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapToReturnToCall()
    }

    val views: ViewCurrentCallsBinding
    var callback: Callback? = null

    init {
        inflate(context, R.layout.view_current_calls, this)
        views = ViewCurrentCallsBinding.bind(this)
        setBackgroundColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        setOnClickListener { callback?.onTapToReturnToCall() }
    }

    fun render(calls: List<WebRtcCall>, formattedDuration: String) {
        val connectedCalls = calls.filter {
            it.mxCall.state is CallState.Connected
        }
        val heldCalls = connectedCalls.filter {
            it.isLocalOnHold || it.remoteOnHold
        }
        if (connectedCalls.size == 1) {
            if (heldCalls.size == 1) {
                views.currentCallsInfo.setText(R.string.call_only_paused)
            } else {
                views.currentCallsInfo.text = resources.getString(R.string.call_only_active, formattedDuration)
            }
        } else {
            if (heldCalls.size > 1) {
                views.currentCallsInfo.text = resources.getString(R.string.call_only_multiple_paused, heldCalls.size)
            } else if (heldCalls.size == 1) {
                views.currentCallsInfo.text = resources.getString(R.string.call_active_and_single_paused, formattedDuration)
            } else {
                views.currentCallsInfo.text = resources.getString(R.string.call_active_and_multiple_paused, formattedDuration, heldCalls.size)
            }
        }
    }
}
