/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotredesign.features.settings.push

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

data class PushRulesViewState(
        val rules: List<PushRule> = emptyList()
) : MvRxState


class PushRulesViewModel(initialState: PushRulesViewState) : VectorViewModel<PushRulesViewState>(initialState) {

    companion object : MvRxViewModelFactory<PushRulesViewModel, PushRulesViewState> {

        override fun initialState(viewModelContext: ViewModelContext): PushRulesViewState? {
            val session = viewModelContext.activity.get<Session>()
            val rules = session.getPushRules()

            return PushRulesViewState(rules)
        }

    }
}