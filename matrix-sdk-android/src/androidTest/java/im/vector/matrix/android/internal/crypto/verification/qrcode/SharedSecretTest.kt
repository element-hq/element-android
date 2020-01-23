/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.crypto.verification.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SharedSecretTest : InstrumentedTest {

    @Test
    fun testSharedSecretLengthCase() {
        val sharedSecret = generateSharedSecret()

        sharedSecret.length shouldBe 43
    }

    @Test
    fun testSharedDiffCase() {
        val sharedSecret1 = generateSharedSecret()
        val sharedSecret2 = generateSharedSecret()

        sharedSecret1 shouldNotBeEqualTo sharedSecret2
    }
}
