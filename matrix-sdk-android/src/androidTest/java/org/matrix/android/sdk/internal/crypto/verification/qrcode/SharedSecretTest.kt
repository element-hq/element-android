/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SharedSecretTest : InstrumentedTest {

    @Test
    fun testSharedSecretLengthCase() {
        repeat(100) {
            generateSharedSecretV2().length shouldBe 11
        }
    }

    @Test
    fun testSharedDiffCase() {
        val sharedSecret1 = generateSharedSecretV2()
        val sharedSecret2 = generateSharedSecretV2()

        sharedSecret1 shouldNotBeEqualTo sharedSecret2
    }
}
