/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.devtools

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.flow.flow

data class AccountDataViewState(
        val accountData: Async<List<UserAccountDataEvent>> = Uninitialized
) : MavericksState

class AccountDataViewModel @AssistedInject constructor(@Assisted initialState: AccountDataViewState,
                                                       private val session: Session) :
    VectorViewModel<AccountDataViewState, AccountDataAction, EmptyViewEvents>(initialState) {

    init {
        session.flow().liveUserAccountData(emptySet())
                .execute {
                    copy(accountData = it)
                }
    }

    override fun handle(action: AccountDataAction) {
        when (action) {
            is AccountDataAction.DeleteAccountData -> handleDeleteAccountData(action)
        }
    }

    private fun handleDeleteAccountData(action: AccountDataAction.DeleteAccountData) {
        viewModelScope.launch {
            session.accountDataService().updateUserAccountData(action.type, emptyMap())
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AccountDataViewModel, AccountDataViewState> {
        override fun create(initialState: AccountDataViewState): AccountDataViewModel
    }

    companion object : MavericksViewModelFactory<AccountDataViewModel, AccountDataViewState> by hiltMavericksViewModelFactory()
}
