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
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

class VectorSettingsPushRuleNotificationPreferenceViewModel @AssistedInject constructor(
        @Assisted initialState: VectorSettingsPushRuleNotificationPreferenceViewState,
        private val activeSessionHolder: ActiveSessionHolder,
) : VectorViewModel<VectorSettingsPushRuleNotificationPreferenceViewState,
        VectorSettingsPushRuleNotificationPreferenceViewAction,
        VectorSettingsPushRuleNotificationPreferenceViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VectorSettingsPushRuleNotificationPreferenceViewModel, VectorSettingsPushRuleNotificationPreferenceViewState> {
        override fun create(initialState: VectorSettingsPushRuleNotificationPreferenceViewState): VectorSettingsPushRuleNotificationPreferenceViewModel
    }

    companion object : MavericksViewModelFactory<VectorSettingsPushRuleNotificationPreferenceViewModel, VectorSettingsPushRuleNotificationPreferenceViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: VectorSettingsPushRuleNotificationPreferenceViewAction) {
        when (action) {
            is VectorSettingsPushRuleNotificationPreferenceViewAction.UpdatePushRule -> handleUpdatePushRule(action.pushRuleAndKind, action.checked)
        }
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
            runCatching {
                activeSessionHolder.getSafeActiveSession()?.pushRuleService()?.updatePushRuleActions(
                        kind = kind,
                        ruleId = ruleId,
                        enable = enabled,
                        actions = newActions
                )
            }.fold(
                    onSuccess = {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(VectorSettingsPushRuleNotificationPreferenceViewEvent.PushRuleUpdated(ruleId, checked))
                    },
                    onFailure = { failure ->
                        setState { copy(isLoading = false) }
                        _viewEvents.post(VectorSettingsPushRuleNotificationPreferenceViewEvent.Failure(failure))
                    }
            )
        }
    }
}
