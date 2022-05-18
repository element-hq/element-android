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
import org.matrix.android.sdk.api.auth.LoginType

class LoginTypeTest {

    @Test
    fun `when getting type fromName, then map correctly`() {
        LoginType.fromName(LoginType.PASSWORD.name) shouldBeEqualTo LoginType.PASSWORD
        LoginType.fromName(LoginType.SSO.name) shouldBeEqualTo LoginType.SSO
        LoginType.fromName(LoginType.UNSUPPORTED.name) shouldBeEqualTo LoginType.UNSUPPORTED
        LoginType.fromName(LoginType.CUSTOM.name) shouldBeEqualTo LoginType.CUSTOM
        LoginType.fromName(LoginType.DIRECT.name) shouldBeEqualTo LoginType.DIRECT
        LoginType.fromName(LoginType.UNKNOWN.name) shouldBeEqualTo LoginType.UNKNOWN
    }

    @Test // The failure of this test means that an existing type has not been correctly added to fromValue
    fun `given non-unknown type name, when getting type fromName, then type is not UNKNOWN`() {
        val types = LoginType.values()

        types.forEach { type ->
            if (type != LoginType.UNKNOWN) {
                LoginType.fromName(type.name) shouldNotBeEqualTo LoginType.UNKNOWN
            }
        }
    }
}
