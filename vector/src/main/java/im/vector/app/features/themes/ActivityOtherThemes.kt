/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.themes

import androidx.annotation.StyleRes
import im.vector.lib.ui.styles.R

/**
 * Class to manage Activity other possible themes.
 * Note that style for light theme is default and is declared in the Android Manifest
 */
sealed class ActivityOtherThemes(
        @StyleRes val dark: Int,
        @StyleRes val black: Int
) {

    object Default : ActivityOtherThemes(
            R.style.Theme_Vector_Dark,
            R.style.Theme_Vector_Black
    )

    object Launcher : ActivityOtherThemes(
            R.style.Theme_Vector_Launcher,
            R.style.Theme_Vector_Launcher
    )

    object AttachmentsPreview : ActivityOtherThemes(
            R.style.Theme_Vector_Black_AttachmentsPreview,
            R.style.Theme_Vector_Black_AttachmentsPreview
    )

    object VectorAttachmentsPreview : ActivityOtherThemes(
            R.style.Theme_Vector_Black_Transparent,
            R.style.Theme_Vector_Black_Transparent
    )
}
