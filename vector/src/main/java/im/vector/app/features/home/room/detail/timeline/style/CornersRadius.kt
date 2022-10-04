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
