/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.graphics.drawable.Drawable
import androidx.annotation.Px

data class MapState(
        val zoomOnlyOnce: Boolean,
        val pinLocationData: LocationData? = null,
        val pinId: String,
        val pinDrawable: Drawable? = null,
        val showPin: Boolean = true,
        val userLocationData: LocationData? = null,
        @Px val logoMarginBottom: Int = 0,
)
