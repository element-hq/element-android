/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.granularRoundedCorners
import im.vector.app.features.themes.ThemeUtils

/**
 * Default implementation of common methods for item representing the status of a live location share.
 */
class DefaultLiveLocationShareStatusItem : LiveLocationShareStatusItem {

    override fun bindMap(
            mapImageView: ImageView,
            mapWidth: Int,
            mapHeight: Int,
            messageLayout: TimelineMessageLayout
    ) {
        val mapCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            messageLayout.cornersRadius.granularRoundedCorners()
        } else {
            RoundedCorners(getDefaultLayoutCornerRadiusInDp(mapImageView.resources))
        }
        mapImageView.updateLayoutParams {
            width = mapWidth
            height = mapHeight
        }
        GlideApp.with(mapImageView)
                .load(ContextCompat.getDrawable(mapImageView.context, R.drawable.bg_no_location_map))
                .transform(MultiTransformation(CenterCrop(), mapCornerTransformation))
                .into(mapImageView)
    }

    override fun bindBottomBanner(bannerImageView: ImageView, messageLayout: TimelineMessageLayout) {
        val imageCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            GranularRoundedCorners(
                    0f,
                    0f,
                    messageLayout.cornersRadius.bottomEndRadius,
                    messageLayout.cornersRadius.bottomStartRadius
            )
        } else {
            val bottomCornerRadius = getDefaultLayoutCornerRadiusInDp(bannerImageView.resources).toFloat()
            GranularRoundedCorners(0f, 0f, bottomCornerRadius, bottomCornerRadius)
        }
        GlideApp.with(bannerImageView)
                .load(ColorDrawable(ThemeUtils.getColor(bannerImageView.context, android.R.attr.colorBackground)))
                .transform(imageCornerTransformation)
                .into(bannerImageView)
    }

    private fun getDefaultLayoutCornerRadiusInDp(resources: Resources): Int {
        val dimensionConverter = DimensionConverter(resources)
        return dimensionConverter.dpToPx(8)
    }
}
