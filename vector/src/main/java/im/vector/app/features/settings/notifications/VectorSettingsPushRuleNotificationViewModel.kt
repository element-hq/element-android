/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.settings.notifications.VectorSettingsPushRuleNotificationViewEvent.Failure
import im.vector.app.features.settings.notifications.VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.Failure.ServerError
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

private typealias ViewModel = VectorSettingsPushRuleNotificationViewModel
private typealias ViewState = VectorSettingsPushRuleNotificationViewState

class VectorSettingsPushRuleNotificationViewModel @AssistedInject constructor(
        @Assisted initialState: ViewState,
        private val activeSessionHolder: ActiveSessionHolder,
) : VectorViewModel<VectorSettingsPushRuleNotificationViewState,
        VectorSettingsPushRuleNotificationViewAction,
        VectorSettingsPushRuleNotificationViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ViewModel, ViewState> {
        override fun create(initialState: ViewState): ViewModel
    }

    companion object : MavericksViewModelFactory<ViewModel, ViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: VectorSettingsPushRuleNotificationViewAction) {
        when (action) {
            is VectorSettingsPushRuleNotificationViewAction.UpdatePushRule -> handleUpdatePushRule(action.pushRuleAndKind, action.checked)
        }
    }

    fun getPushRuleAndKind(ruleId: String): PushRuleAndKind? {
        return activeSessionHolder.getSafeActiveSession()?.pushRuleService()?.getPushRules()?.findDefaultRule(ruleId)
    }

    fun isPushRuleChecked(ruleId: String): Boolean {
        val rulesGroup = listOf(ruleId) + RuleIds.getSyncedRules(ruleId)
        return rulesGroup.mapNotNull { getPushRuleAndKind(it) }.any { it.pushRule.notificationIndex != NotificationIndex.OFF }
    }

    private fun handleUpdatePushRule(pushRuleAndKind: PushRuleAndKind, checked: Boolean) {
        val ruleId = pushRuleAndKind.pushRule.ruleId
        val kind = pushRuleAndKind.kind
        val newIndex = if (checked) NotificationIndex.NOISY else NotificationIndex.OFF
        val standardAction = getStandardAction(ruleId, newIndex) ?: return
        val enabled = standardAction != StandardActions.Disabled
        val newActions = standardAction.actions
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            val rulesToUpdate = listOf(ruleId) + RuleIds.getSyncedRules(ruleId)
            val results = rulesToUpdate.map { ruleId ->
                runCatching {
                    updatePushRule(kind, ruleId, enabled, newActions)
                }
            }
            setState { copy(isLoading = false) }
            val failure = results.firstNotNullOfOrNull { result ->
                // If the failure is a rule not found error, do not consider it
                result.exceptionOrNull()?.takeUnless { it is ServerError && it.error.code == MatrixError.M_NOT_FOUND }
            }
            val newChecked = if (checked) {
                // If any rule is checked, the global rule is checked
                results.any { it.isSuccess }
            } else {
                // If any rule has not been unchecked, the global rule remains checked
                failure != null
            }
            if (results.any { it.isSuccess }) {
                _viewEvents.post(PushRuleUpdated(ruleId, newChecked, failure))
            } else {
                _viewEvents.post(Failure(failure))
            }
        }
    }

    private suspend fun updatePushRule(kind: RuleKind, ruleId: String, enable: Boolean, newActions: List<Action>?) {
        activeSessionHolder.getSafeActiveSession()?.pushRuleService()?.updatePushRuleActions(
                kind = kind,
                ruleId = ruleId,
                enable = enable,
                actions = newActions
        )
    }
}
