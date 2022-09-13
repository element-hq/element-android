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

import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialog

fun BottomSheetDialog.setPeekHeightAsScreenPercentage(@FloatRange(from = 0.0, to = 1.0) percentage: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setPeekHeightPostApi30(percentage)
    } else {
        setPeekHeightPreApi30(percentage)
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun BottomSheetDialog.setPeekHeightPostApi30(percentage: Float) {
    window?.windowManager?.currentWindowMetrics?.let { windowMetrics ->
        val height = windowMetrics.bounds.height()
        behavior.setPeekHeight((height * percentage).toInt(), true)
    }
}

private fun BottomSheetDialog.setPeekHeightPreApi30(percentage: Float) {
    val displayMetrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    val height = displayMetrics.heightPixels
    behavior.setPeekHeight((height * percentage).toInt(), true)
}
