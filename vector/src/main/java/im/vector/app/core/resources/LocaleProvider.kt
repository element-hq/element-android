/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.resources

import android.content.res.Resources
import android.text.TextUtils
import android.view.View
import androidx.core.os.ConfigurationCompat
import java.util.Locale
import javax.inject.Inject

class LocaleProvider @Inject constructor(private val resources: Resources) {

    fun current(): Locale {
        return ConfigurationCompat.getLocales(resources.configuration)[0]
    }
}

fun LocaleProvider.isEnglishSpeaking() = current().language.startsWith("en")

fun LocaleProvider.getLayoutDirectionFromCurrentLocale() =  TextUtils.getLayoutDirectionFromLocale(current())

fun LocaleProvider.isRTL() = getLayoutDirectionFromCurrentLocale() == View.LAYOUT_DIRECTION_RTL
