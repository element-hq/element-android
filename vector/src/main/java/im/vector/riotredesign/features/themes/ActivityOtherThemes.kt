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

package im.vector.ui.themes

import androidx.annotation.StyleRes
import im.vector.riotredesign.R

/**
 * Class to manage Activity other possible themes.
 * Note that style for light theme is default and is declared in the Android Manifest
 */
sealed class ActivityOtherThemes(@StyleRes val dark: Int,
                                 @StyleRes val black: Int,
                                 @StyleRes val status: Int) {

    object Default : ActivityOtherThemes(
            R.style.AppTheme_Dark,
            R.style.AppTheme_Black,
            R.style.AppTheme_Status
    )

    object NoActionBarFullscreen : ActivityOtherThemes(
            R.style.AppTheme_NoActionBar_FullScreen_Dark,
            R.style.AppTheme_NoActionBar_FullScreen_Black,
            R.style.AppTheme_NoActionBar_FullScreen_Status
    )

    object Home : ActivityOtherThemes(
            R.style.HomeActivityTheme_Dark,
            R.style.HomeActivityTheme_Black,
            R.style.HomeActivityTheme_Status
    )

    object Group : ActivityOtherThemes(
            R.style.GroupAppTheme_Dark,
            R.style.GroupAppTheme_Black,
            R.style.GroupAppTheme_Status
    )

    object Picker : ActivityOtherThemes(
            R.style.CountryPickerTheme_Dark,
            R.style.CountryPickerTheme_Black,
            R.style.CountryPickerTheme_Status
    )

    object Lock : ActivityOtherThemes(
            R.style.Theme_Vector_Lock_Dark,
            R.style.Theme_Vector_Lock_Light,
            R.style.Theme_Vector_Lock_Status
    )

    object Search : ActivityOtherThemes(
            R.style.SearchesAppTheme_Dark,
            R.style.SearchesAppTheme_Black,
            R.style.SearchesAppTheme_Status
    )

    object Call : ActivityOtherThemes(
            R.style.CallActivityTheme_Dark,
            R.style.CallActivityTheme_Black,
            R.style.CallActivityTheme_Status
    )
}