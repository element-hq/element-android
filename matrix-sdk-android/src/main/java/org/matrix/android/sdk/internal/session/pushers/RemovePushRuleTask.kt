/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface RemovePushRuleTask : Task<RemovePushRuleTask.Params, Unit> {
    data class Params(
            val kind: RuleKind,
            val ruleId: String
    )
}

internal class DefaultRemovePushRuleTask @Inject constructor(
        private val pushRulesApi: PushRulesApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : RemovePushRuleTask {

    override suspend fun execute(params: RemovePushRuleTask.Params) {
        return executeRequest(globalErrorReceiver) {
            pushRulesApi.deleteRule(params.kind.value, params.ruleId)
        }
    }
}
