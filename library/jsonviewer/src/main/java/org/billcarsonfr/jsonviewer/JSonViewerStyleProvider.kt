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

package org.billcarsonfr.jsonviewer

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class JSonViewerStyleProvider(
    @ColorInt val keyColor: Int,
    @ColorInt val stringColor: Int,
    @ColorInt val booleanColor: Int,
    @ColorInt val numberColor: Int,
    @ColorInt val baseColor: Int,
    @ColorInt val secondaryColor: Int
) : Parcelable {

    companion object {
        fun default(context: Context) = JSonViewerStyleProvider(
            keyColor = ContextCompat.getColor(context, R.color.key_color),
            stringColor = ContextCompat.getColor(context, R.color.string_color),
            booleanColor = ContextCompat.getColor(context, R.color.bool_color),
            numberColor = ContextCompat.getColor(context, R.color.number_color),
            baseColor = ContextCompat.getColor(context, R.color.base_color),
            secondaryColor = ContextCompat.getColor(context, R.color.secondary_color)
        )
    }
}
