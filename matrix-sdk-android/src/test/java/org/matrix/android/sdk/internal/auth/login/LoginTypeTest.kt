/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth.login

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Test

class LoginTypeTest {

    @Test
    fun `when getting type fromValue, then map correctly`() {
        LoginType.fromValue(LoginType.PASSWORD.value) shouldBeEqualTo LoginType.PASSWORD
        LoginType.fromValue(LoginType.SSO.value) shouldBeEqualTo LoginType.SSO
        LoginType.fromValue(LoginType.UNSUPPORTED.value) shouldBeEqualTo LoginType.UNSUPPORTED
        LoginType.fromValue(LoginType.UNKNOWN.value) shouldBeEqualTo LoginType.UNKNOWN
    }

    @Test // The failure of this test means that an existing type has not been correctly added to fromValue
    fun `given non-unknown type value, when getting type fromValue, then type is not UNKNOWN`() {
        val types = LoginType.values()

        types.forEach { type ->
            if (type != LoginType.UNKNOWN) {
                LoginType.fromValue(type.value) shouldNotBeEqualTo LoginType.UNKNOWN
            }
        }
    }
}
