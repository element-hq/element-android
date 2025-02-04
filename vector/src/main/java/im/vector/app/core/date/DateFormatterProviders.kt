/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.date

import javax.inject.Inject

class DateFormatterProviders @Inject constructor(
        private val defaultDateFormatterProvider: DefaultDateFormatterProvider,
        private val abbrevDateFormatterProvider: AbbrevDateFormatterProvider
) {

    fun provide(abbrev: Boolean): DateFormatterProvider {
        return if (abbrev) {
            abbrevDateFormatterProvider
        } else {
            defaultDateFormatterProvider
        }
    }
}
