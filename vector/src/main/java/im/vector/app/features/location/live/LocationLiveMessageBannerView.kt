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

package im.vector.app.features.location.live

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.TextUtils
import im.vector.app.databinding.ViewLocationLiveMessageBannerBinding
import org.threeten.bp.Duration

data class LocationLiveMessageBannerViewState(
        val isStopButtonVisible: Boolean,
        val remainingTimeInMillis: Long
)

class LocationLiveMessageBannerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLocationLiveMessageBannerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    val stopButton: Button
        get() = binding.locationLiveMessageBannerStop

    private val subTitle: TextView
        get() = binding.locationLiveMessageBannerSubTitle

    fun render(viewState: LocationLiveMessageBannerViewState) {
        stopButton.isVisible = viewState.isStopButtonVisible
        val duration = Duration.ofMillis(viewState.remainingTimeInMillis.coerceAtLeast(0L))
        subTitle.text = context.getString(R.string.location_share_live_remaining_time, TextUtils.formatDurationWithUnits(context, duration))
    }
}
