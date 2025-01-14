/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
