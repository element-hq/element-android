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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import im.vector.app.R
import kotlinx.android.synthetic.main.view_jump_to_read_marker.view.*

class JumpToReadMarkerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onJumpToReadMarkerClicked()
        fun onClearReadMarkerClicked()
    }

    var callback: Callback? = null

    init {
        setupView()
    }

    private fun setupView() {
        inflate(context, R.layout.view_jump_to_read_marker, this)
        setBackgroundColor(ContextCompat.getColor(context, R.color.notification_accent_color))
        jumpToReadMarkerLabelView.setOnClickListener {
            callback?.onJumpToReadMarkerClicked()
        }
        closeJumpToReadMarkerView.setOnClickListener {
            visibility = View.INVISIBLE
            callback?.onClearReadMarkerClicked()
        }
    }
}
