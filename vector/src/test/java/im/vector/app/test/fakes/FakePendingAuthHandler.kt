/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.auth.PendingAuthHandler
import io.mockk.mockk
import io.mockk.spyk

class FakePendingAuthHandler {

    val instance = spyk(mockk<PendingAuthHandler>())
}
