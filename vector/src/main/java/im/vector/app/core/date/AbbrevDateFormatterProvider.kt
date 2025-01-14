/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.date

import android.text.format.DateFormat
import im.vector.app.core.resources.LocaleProvider
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class AbbrevDateFormatterProvider @Inject constructor(private val localeProvider: LocaleProvider) : DateFormatterProvider {

    override val dateWithMonthFormatter: DateTimeFormatter by lazy {
        val pattern = DateFormat.getBestDateTimePattern(localeProvider.current(), "d MMM")
        DateTimeFormatter.ofPattern(pattern, localeProvider.current())
    }

    override val dateWithYearFormatter: DateTimeFormatter by lazy {
        val pattern = DateFormat.getBestDateTimePattern(localeProvider.current(), "dd.MM.yyyy")
        DateTimeFormatter.ofPattern(pattern, localeProvider.current())
    }
}
