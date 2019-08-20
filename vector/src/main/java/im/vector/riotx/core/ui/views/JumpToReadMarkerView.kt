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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import butterknife.ButterKnife
import im.vector.riotx.R
import im.vector.riotx.features.themes.ThemeUtils
import kotlinx.android.synthetic.main.view_jump_to_read_marker.view.*
import me.gujun.android.span.span
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class JumpToReadMarkerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onJumpToReadMarkerClicked(readMarkerId: String)
        fun onClearReadMarkerClicked()
    }

    var callback: Callback? = null

    init {
        setupView()
    }

    private fun setupView() {
        LinearLayout.inflate(context, R.layout.view_jump_to_read_marker, this)
        setBackgroundColor(ContextCompat.getColor(context, R.color.notification_accent_color))
        jumpToReadMarkerLabelView.movementMethod = BetterLinkMovementMethod.getInstance()
        isClickable = true
        closeJumpToReadMarkerView.setOnClickListener {
            visibility = View.GONE
            callback?.onClearReadMarkerClicked()
        }
    }

    fun render(show: Boolean, readMarkerId: String?) {
        isVisible = show
        if (readMarkerId != null) {
            jumpToReadMarkerLabelView.text = span(resources.getString(R.string.room_jump_to_first_unread)) {
                textDecorationLine = "underline"
                onClick = { callback?.onJumpToReadMarkerClicked(readMarkerId) }
            }
        }

    }


}
