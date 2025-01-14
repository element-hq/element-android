/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.notification.PushRulesUpdater
import io.mockk.justRun
import io.mockk.mockk
import org.matrix.android.sdk.api.session.Session

class FakePushRulesUpdater {

    val instance = mockk<PushRulesUpdater>()

    fun givenOnSessionStarted(session: Session) {
        justRun { instance.onSessionStarted(session) }
    }
}
