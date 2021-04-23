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
 * Note that style for light theme is default and is declared in the Android Manifest
 */
sealed class ActivityOtherThemes(@StyleRes val dark: Int,
                                 @StyleRes val black: Int) {

    object Default : ActivityOtherThemes(
            R.style.AppTheme_Dark,
            R.style.AppTheme_Black
    )

    object Launcher : ActivityOtherThemes(
            R.style.AppTheme_Launcher,
            R.style.AppTheme_Launcher
    )

    object AttachmentsPreview : ActivityOtherThemes(
            R.style.AppTheme_AttachmentsPreview,
            R.style.AppTheme_AttachmentsPreview
    )

    object VectorAttachmentsPreview : ActivityOtherThemes(
            R.style.AppTheme_Transparent,
            R.style.AppTheme_Transparent
    )
}
