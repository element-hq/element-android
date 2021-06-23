/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.themes

import androidx.annotation.StyleRes
import im.vector.app.R

/**
 * Class to manage Activity other possible themes.
 * Note that style for light theme is no more default and is no more declared in the Android Manifest
 */
sealed class ActivityOtherThemes(@StyleRes val light: Int,
                                 @StyleRes val dark: Int,
                                 @StyleRes val black: Int,
                                 @StyleRes val sc_light: Int,
                                 @StyleRes val sc: Int,
                                 @StyleRes val sc_dark: Int,
                                 @StyleRes val sc_colored: Int,
                                 @StyleRes val sc_dark_colored: Int) {

    object Default : ActivityOtherThemes(
            R.style.Theme_Vector_Light,
            R.style.Theme_Vector_Dark,
            R.style.Theme_Vector_Black,
            R.style.AppTheme_SC_Light,
            R.style.AppTheme_SC,
            R.style.AppTheme_SC_Dark,
            R.style.AppTheme_SC_Colored,
            R.style.AppTheme_SC_Dark_Colored
    )

    object Launcher : ActivityOtherThemes(
            R.style.Theme_Vector_Launcher,
            R.style.Theme_Vector_Launcher,
            R.style.Theme_Vector_Launcher,
            R.style.AppTheme_Launcher_SC,
            R.style.AppTheme_Launcher_SC,
            R.style.AppTheme_Launcher_SC,
            R.style.AppTheme_Launcher_SC,
            R.style.AppTheme_Launcher_SC,
    )

    object AttachmentsPreview : ActivityOtherThemes(
            R.style.Theme_Vector_Black_AttachmentsPreview,
            R.style.Theme_Vector_Black_AttachmentsPreview,
            R.style.Theme_Vector_Black_AttachmentsPreview,
            R.style.AppTheme_AttachmentsPreview_SC,
            R.style.AppTheme_AttachmentsPreview_SC,
            R.style.AppTheme_AttachmentsPreview_SC,
            R.style.AppTheme_AttachmentsPreview_SC,
            R.style.AppTheme_AttachmentsPreview_SC
    )

    object VectorAttachmentsPreview : ActivityOtherThemes(
            R.style.Theme_Vector_Black_Transparent,
            R.style.Theme_Vector_Black_Transparent,
            R.style.Theme_Vector_Black_Transparent,
            R.style.AppTheme_Transparent_SC,
            R.style.AppTheme_Transparent_SC,
            R.style.AppTheme_Transparent_SC,
            R.style.AppTheme_Transparent_SC,
            R.style.AppTheme_Transparent_SC
    )
}
