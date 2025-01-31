/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.crosssigning

import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.Test
import org.matrix.android.sdk.api.util.fromBase64

@Suppress("SpellCheckingInspection")
class ExtensionsKtTest {

    @Test
    fun testComparingBase64StringWithOrWithoutPadding() {
        // Without padding
        "NMJyumnhMic".fromBase64().contentEquals("NMJyumnhMic".fromBase64()).shouldBeTrue()
        // With padding
        "NMJyumnhMic".fromBase64().contentEquals("NMJyumnhMic=".fromBase64()).shouldBeTrue()
    }

    @Test
    fun testBadBase64() {
        "===".fromBase64Safe().shouldBeNull()
    }
}
