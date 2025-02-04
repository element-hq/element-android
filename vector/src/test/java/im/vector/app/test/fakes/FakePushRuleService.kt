/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import org.matrix.android.sdk.api.session.pushrules.PushRuleService

class FakePushRuleService : PushRuleService by mockk(relaxed = true) {

    fun givenUpdatePushRuleActionsSucceed(ruleId: String? = null) {
        coJustRun { updatePushRuleActions(any(), ruleId ?: any(), any(), any()) }
    }

    fun givenUpdatePushRuleActionsFail(ruleId: String? = null, failure: Throwable = mockk()) {
        coEvery { updatePushRuleActions(any(), ruleId ?: any(), any(), any()) }.throws(failure)
    }
}
