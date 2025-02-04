/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.style

import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

fun TimelineMessageLayout.Bubble.CornersRadius.granularRoundedCorners(): GranularRoundedCorners {
    return GranularRoundedCorners(topStartRadius, topEndRadius, bottomEndRadius, bottomStartRadius)
}

fun TimelineMessageLayout.Bubble.CornersRadius.shapeAppearanceModel(): ShapeAppearanceModel {
    return ShapeAppearanceModel().toBuilder()
            .setTopRightCorner(topEndRadius.cornerFamily(), topEndRadius)
            .setBottomRightCorner(bottomEndRadius.cornerFamily(), bottomEndRadius)
            .setTopLeftCorner(topStartRadius.cornerFamily(), topStartRadius)
            .setBottomLeftCorner(bottomStartRadius.cornerFamily(), bottomStartRadius)
            .build()
}

private fun Float.cornerFamily(): Int {
    return if (this == 0F) CornerFamily.CUT else CornerFamily.ROUNDED
}
