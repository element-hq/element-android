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

import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

fun TimelineMessageLayout.Bubble.shapeAppearanceModel(cornerRadius: Float): ShapeAppearanceModel {
    val (topCornerFamily, topRadius) = if (isFirstFromThisSender) {
        Pair(CornerFamily.ROUNDED, cornerRadius)
    } else {
        Pair(CornerFamily.CUT, 0f)
    }
    val (bottomCornerFamily, bottomRadius) = if (isLastFromThisSender) {
        Pair(CornerFamily.ROUNDED, cornerRadius)
    } else {
        Pair(CornerFamily.CUT, 0f)
    }
    val shapeAppearanceModelBuilder = ShapeAppearanceModel().toBuilder()
    if (isIncoming) {
        shapeAppearanceModelBuilder
                .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
                .setTopLeftCorner(topCornerFamily, topRadius)
                .setBottomLeftCorner(bottomCornerFamily, bottomRadius)
    } else {
        shapeAppearanceModelBuilder
                .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                .setTopRightCorner(topCornerFamily, topRadius)
                .setBottomRightCorner(bottomCornerFamily, bottomRadius)
    }
    return shapeAppearanceModelBuilder.build()
}
