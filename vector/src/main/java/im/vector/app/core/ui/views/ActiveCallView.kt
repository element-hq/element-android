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
import im.vector.app.features.call.webrtc.WebRtcCall
import im.vector.app.features.themes.ThemeUtils
import kotlinx.android.synthetic.main.view_active_call_view.view.*
import org.matrix.android.sdk.api.session.call.CallState

class ActiveCallView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapToReturnToCall()
    }

    var callback: Callback? = null

    init {
        setupView()
    }

    private fun setupView() {
        inflate(context, R.layout.view_active_call_view, this)
        setBackgroundColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        setOnClickListener { callback?.onTapToReturnToCall() }
    }

    fun render(calls: List<WebRtcCall>) {
        if (calls.size == 1) {
            activeCallInfo.setText(R.string.call_active_call)
        } else if (calls.size == 2) {
            val activeCall = calls.firstOrNull {
                it.mxCall.state is CallState.Connected && !it.isLocalOnHold()
            }
            if (activeCall == null) {
                activeCallInfo.setText(R.string.call_two_paused_calls)
            } else {
                activeCallInfo.setText(R.string.call_one_active_one_paused_call)
            }
        } else {
            visibility = GONE
        }
    }
}
