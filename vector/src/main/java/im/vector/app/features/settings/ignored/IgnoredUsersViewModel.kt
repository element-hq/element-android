/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.settings.ignored

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.flow.flow

data class IgnoredUsersViewState(
        val ignoredUsers: List<User> = emptyList(),
        val unIgnoreRequest: Async<Unit> = Uninitialized
) : MavericksState

sealed class IgnoredUsersAction : VectorViewModelAction {
    data class UnIgnore(val userId: String) : IgnoredUsersAction()
}

class IgnoredUsersViewModel @AssistedInject constructor(@Assisted initialState: IgnoredUsersViewState,
                                                        private val session: Session) :
    VectorViewModel<IgnoredUsersViewState, IgnoredUsersAction, IgnoredUsersViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<IgnoredUsersViewModel, IgnoredUsersViewState> {
        override fun create(initialState: IgnoredUsersViewState): IgnoredUsersViewModel
    }

    companion object : MavericksViewModelFactory<IgnoredUsersViewModel, IgnoredUsersViewState> by hiltMavericksViewModelFactory()

    init {
        observeIgnoredUsers()
    }

    private fun observeIgnoredUsers() {
        session.flow()
                .liveIgnoredUsers()
                .execute { async ->
                    copy(
                            ignoredUsers = async.invoke().orEmpty()
                    )
                }
    }

    override fun handle(action: IgnoredUsersAction) {
        when (action) {
            is IgnoredUsersAction.UnIgnore -> handleUnIgnore(action)
        }
    }

    private fun handleUnIgnore(action: IgnoredUsersAction.UnIgnore) {
        setState {
            copy(
                    unIgnoreRequest = Loading()
            )
        }

        viewModelScope.launch {
            val result = runCatching { session.unIgnoreUserIds(listOf(action.userId)) }
            setState {
                copy(
                        unIgnoreRequest = result.fold(::Success, ::Fail)
                )
            }
            result.onFailure { _viewEvents.post(IgnoredUsersViewEvents.Failure(it)) }
        }
    }
}
