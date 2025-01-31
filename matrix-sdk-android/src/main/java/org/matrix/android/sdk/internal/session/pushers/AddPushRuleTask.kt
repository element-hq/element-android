/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface AddPushRuleTask : Task<AddPushRuleTask.Params, Unit> {
    data class Params(
            val kind: RuleKind,
            val pushRule: PushRule
    )
}

internal class DefaultAddPushRuleTask @Inject constructor(
        private val pushRulesApi: PushRulesApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddPushRuleTask {

    override suspend fun execute(params: AddPushRuleTask.Params) {
        return executeRequest(globalErrorReceiver) {
            pushRulesApi.addRule(params.kind.value, params.pushRule.ruleId, params.pushRule)
        }
    }
}
