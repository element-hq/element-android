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

package im.vector.app.core.extensions

import android.util.DisplayMetrics
import androidx.annotation.FloatRange
import com.google.android.material.bottomsheet.BottomSheetDialog

@Suppress("DEPRECATION")
fun BottomSheetDialog.setPeekHeightAsScreenPercentage(@FloatRange(from = 0.0, to = 1.0) percentage: Float) {
    val displayMetrics = DisplayMetrics()
    window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    val height = displayMetrics.heightPixels
    behavior.setPeekHeight((height * percentage).toInt(), true)
}
