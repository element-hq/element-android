/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import android.content.res.Resources
import android.text.TextUtils
import android.view.View
import androidx.core.os.ConfigurationCompat
import java.util.Locale
import javax.inject.Inject

interface LocaleProvider {

    fun current(): Locale
}

class DefaultLocaleProvider @Inject constructor(private val resources: Resources) : LocaleProvider {

    override fun current(): Locale {
        return ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.getDefault()
    }
}

fun LocaleProvider.isEnglishSpeaking() = current().language.startsWith("en")

fun LocaleProvider.getLayoutDirectionFromCurrentLocale() = TextUtils.getLayoutDirectionFromLocale(current())

fun LocaleProvider.isRTL() = getLayoutDirectionFromCurrentLocale() == View.LAYOUT_DIRECTION_RTL
