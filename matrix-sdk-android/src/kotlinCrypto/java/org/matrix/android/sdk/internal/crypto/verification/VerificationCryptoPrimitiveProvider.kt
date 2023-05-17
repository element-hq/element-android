/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.internal.crypto.tools.withOlmUtility
import org.matrix.olm.OlmSAS
import javax.inject.Inject

// Mainly for testing purpose to ease mocking
internal class VerificationCryptoPrimitiveProvider @Inject constructor() {

    fun provideOlmSas(): OlmSAS {
        return OlmSAS()
    }

    fun sha256(toHash: String): String {
        return withOlmUtility {
            it.sha256(toHash)
        }
    }
}
