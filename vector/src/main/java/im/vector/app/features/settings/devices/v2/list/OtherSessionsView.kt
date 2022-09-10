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

package im.vector.app.features.settings.devices.v2.list

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.databinding.ViewOtherSessionsBinding
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import javax.inject.Inject

@AndroidEntryPoint
class OtherSessionsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var otherSessionsController: OtherSessionsController

    private val views: ViewOtherSessionsBinding

    init {
        inflate(context, R.layout.view_other_sessions, this)
        views = ViewOtherSessionsBinding.bind(this)
    }

    fun render(devices: List<DeviceFullInfo>) {
        views.otherSessionsRecyclerView.configureWith(otherSessionsController, hasFixedSize = true)
        views.otherSessionsViewAllButton.text = context.getString(R.string.device_manager_other_sessions_view_all, devices.size)
        otherSessionsController.setData(devices)
    }

    fun setCallback(callback: OtherSessionsController.Callback) {
        otherSessionsController.callback = callback
    }

    override fun onDetachedFromWindow() {
        otherSessionsController.callback = null
        views.otherSessionsRecyclerView.cleanup()
        super.onDetachedFromWindow()
    }
}
