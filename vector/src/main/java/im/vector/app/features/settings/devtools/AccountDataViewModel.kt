/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

class AccountDataViewModel @AssistedInject constructor(
        @Assisted initialState: AccountDataViewState,
        private val session: Session
) :
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
