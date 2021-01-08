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
        if (connectedCalls.isEmpty()) return
        views.currentCallsInfo.text = if (connectedCalls.size == heldCalls.size) {
            resources.getQuantityString(R.plurals.call_only_paused, heldCalls.size, heldCalls.size)
        } else {
            if (heldCalls.isEmpty()) {
                resources.getString(R.string.call_only_active, formattedDuration)
            } else {
                resources.getQuantityString(R.plurals.call_one_active_and_other_paused, heldCalls.size, formattedDuration, heldCalls.size)
            }
        }
    }
}
