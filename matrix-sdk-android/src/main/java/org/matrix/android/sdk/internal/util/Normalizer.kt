/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import java.text.Normalizer
import javax.inject.Inject

internal class Normalizer @Inject constructor() {

    fun normalize(input: String): String {
        return Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
    }
}
