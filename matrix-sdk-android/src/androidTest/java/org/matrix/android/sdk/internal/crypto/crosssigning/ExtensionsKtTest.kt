/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
