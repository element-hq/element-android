/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.locale

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.settings.VectorLocale
import java.util.Locale

data class LocalePickerViewState(
        val currentLocale: Locale = VectorLocale.applicationLocale,
        val locales: Async<List<Locale>> = Uninitialized
) : MavericksState
