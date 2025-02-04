/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.user.model.User

class FakeUserService : UserService by mockk() {

    private val userIdSlot = slot<String>()

    init {
        every { getUser(capture(userIdSlot)) } answers { User(userId = userIdSlot.captured) }
    }
}
