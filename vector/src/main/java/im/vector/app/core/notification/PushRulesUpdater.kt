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

package im.vector.app.core.notification

import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.notifications.usecase.UpdatePushRulesIfNeededUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listen changes in Account Data to update the push rules if needed.
 */
@Singleton
class PushRulesUpdater @Inject constructor(
        private val updatePushRulesIfNeededUseCase: UpdatePushRulesIfNeededUseCase,
) {

    private var job: Job? = null

    fun onSessionStarted(session: Session) {
        updatePushRulesOnChange(session)
    }

    private fun updatePushRulesOnChange(session: Session) {
        job?.cancel()
        job = session.coroutineScope.launch {
            session.flow()
                    .liveUserAccountData(UserAccountDataTypes.TYPE_PUSH_RULES)
                    .onEach { updatePushRulesIfNeededUseCase.execute(session) }
                    .collect()
        }
    }
}
