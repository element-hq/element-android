/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.api.session.pushrules.toJson
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdatePushRuleActionsTask : Task<UpdatePushRuleActionsTask.Params, Unit> {
    data class Params(
            val kind: RuleKind,
            val ruleId: String,
            val enable: Boolean,
            val actions: List<Action>?
    )
}

internal class DefaultUpdatePushRuleActionsTask @Inject constructor(
        private val pushRulesApi: PushRulesApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdatePushRuleActionsTask {

    override suspend fun execute(params: UpdatePushRuleActionsTask.Params) {
        executeRequest(globalErrorReceiver) {
            pushRulesApi.updateEnableRuleStatus(
                    params.kind.value,
                    params.ruleId,
                    EnabledBody(params.enable)
            )
        }
        if (params.actions != null) {
            val body = mapOf("actions" to params.actions.toJson())
            executeRequest(globalErrorReceiver) {
                pushRulesApi.updateRuleActions(params.kind.value, params.ruleId, body)
            }
        }
    }
}
