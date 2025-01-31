/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
