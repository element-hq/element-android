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
import im.vector.app.features.themes.ThemeUtils

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
}
