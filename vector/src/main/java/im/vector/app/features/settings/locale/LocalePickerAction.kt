/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.locale

import im.vector.app.core.platform.VectorViewModelAction
import java.util.Locale

sealed class LocalePickerAction : VectorViewModelAction {
    data class SelectLocale(val locale: Locale) : LocalePickerAction()
}
