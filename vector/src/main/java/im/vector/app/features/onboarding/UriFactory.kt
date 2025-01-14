/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.net.Uri
import javax.inject.Inject

class UriFactory @Inject constructor() {

    fun parse(value: String): Uri {
        return Uri.parse(value)
    }
}
