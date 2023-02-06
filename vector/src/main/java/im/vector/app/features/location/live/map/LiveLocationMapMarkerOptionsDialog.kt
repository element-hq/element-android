/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.location.live.map

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import im.vector.app.R
import im.vector.app.databinding.ViewLiveLocationMarkerPopupBinding

class LiveLocationMapMarkerOptionsDialog(
        context: Context,
) : PopupWindow() {

    interface Callback {
        fun onShareLocationClicked()
    }

    private val views: ViewLiveLocationMarkerPopupBinding

    var callback: Callback? = null

    init {
        contentView = View.inflate(context, R.layout.view_live_location_marker_popup, null)

        views = ViewLiveLocationMarkerPopupBinding.bind(contentView)

        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        isFocusable = true
        isTouchable = true

        contentView.setOnClickListener {
            callback?.onShareLocationClicked()
        }
    }

    fun show(anchorView: View) {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        // By default the left side of the dialog is aligned with the pin. We need shift it to the left to make it's center aligned with the pin.
        showAsDropDown(anchorView, -contentView.measuredWidth / 2, 0)
    }
}
