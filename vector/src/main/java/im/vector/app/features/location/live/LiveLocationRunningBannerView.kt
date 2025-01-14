/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.TextUtils
import im.vector.app.databinding.ViewLiveLocationRunningBannerBinding
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import org.threeten.bp.Duration

private const val REMAINING_TIME_COUNTER_INTERVAL_IN_MS = 1000L

class LiveLocationRunningBannerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLiveLocationRunningBannerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    val stopButton: Button
        get() = binding.liveLocationRunningBannerStop

    private val background: ImageView
        get() = binding.liveLocationRunningBannerBackground

    private val title: TextView
        get() = binding.liveLocationRunningBannerTitle

    private val subTitle: TextView
        get() = binding.liveLocationRunningBannerSubTitle

    private var countDownTimer: CountDownTimer? = null

    fun render(viewState: LiveLocationMessageBannerViewState) {
        when (viewState) {
            is LiveLocationMessageBannerViewState.Emitter -> renderEmitter(viewState)
            is LiveLocationMessageBannerViewState.Watcher -> renderWatcher(viewState)
        }

        GlideApp.with(context)
                .load(ColorDrawable(ThemeUtils.getColor(context, android.R.attr.colorBackground)))
                .placeholder(binding.liveLocationRunningBannerBackground.drawable)
                .transform(GranularRoundedCorners(0f, 0f, viewState.bottomEndCornerRadiusInDp, viewState.bottomStartCornerRadiusInDp))
                .into(background)
    }

    private fun renderEmitter(viewState: LiveLocationMessageBannerViewState.Emitter) {
        stopButton.isVisible = true
        title.text = context.getString(CommonStrings.location_share_live_enabled)

        countDownTimer?.cancel()
        viewState.remainingTimeInMillis
                .takeIf { it >= 0 }
                ?.let {
                    countDownTimer = object : CountDownTimer(it, REMAINING_TIME_COUNTER_INTERVAL_IN_MS) {
                        override fun onTick(millisUntilFinished: Long) {
                            val duration = Duration.ofMillis(millisUntilFinished.coerceAtLeast(0L))
                            subTitle.text = context.getString(
                                    CommonStrings.location_share_live_remaining_time,
                                    TextUtils.formatDurationWithUnits(context, duration)
                            )
                        }

                        override fun onFinish() {
                            subTitle.text = context.getString(
                                    CommonStrings.location_share_live_remaining_time,
                                    TextUtils.formatDurationWithUnits(context, Duration.ofMillis(0L))
                            )
                        }
                    }
                    countDownTimer?.start()
                }

        val rootLayout: ConstraintLayout? = (binding.root as? ConstraintLayout)
        rootLayout?.let { parentLayout ->
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootLayout)

            if (viewState.isStopButtonCenteredVertically) {
                constraintSet.connect(
                        R.id.liveLocationRunningBannerStop,
                        ConstraintSet.BOTTOM,
                        R.id.liveLocationRunningBannerBackground,
                        ConstraintSet.BOTTOM,
                        0
                )
            } else {
                constraintSet.clear(R.id.liveLocationRunningBannerStop, ConstraintSet.BOTTOM)
            }

            constraintSet.applyTo(parentLayout)
        }
    }

    private fun renderWatcher(viewState: LiveLocationMessageBannerViewState.Watcher) {
        stopButton.isVisible = false
        title.text = context.getString(CommonStrings.location_share_live_view)
        subTitle.text = context.getString(CommonStrings.location_share_live_until, viewState.formattedLocalTimeOfEndOfLive)
    }

    override fun onDetachedFromWindow() {
        countDownTimer?.cancel()
        super.onDetachedFromWindow()
    }
}
