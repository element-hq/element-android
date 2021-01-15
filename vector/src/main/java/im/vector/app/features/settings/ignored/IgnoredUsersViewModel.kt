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
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.rx.rx

data class IgnoredUsersViewState(
        val ignoredUsers: List<User> = emptyList(),
        val unIgnoreRequest: Async<Unit> = Uninitialized
) : MvRxState

sealed class IgnoredUsersAction : VectorViewModelAction {
    data class UnIgnore(val userId: String) : IgnoredUsersAction()
}

class IgnoredUsersViewModel @AssistedInject constructor(@Assisted initialState: IgnoredUsersViewState,
                                                        private val session: Session)
    : VectorViewModel<IgnoredUsersViewState, IgnoredUsersAction, IgnoredUsersViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: IgnoredUsersViewState): IgnoredUsersViewModel
    }

    companion object : MvRxViewModelFactory<IgnoredUsersViewModel, IgnoredUsersViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: IgnoredUsersViewState): IgnoredUsersViewModel? {
            val ignoredUsersFragment: VectorSettingsIgnoredUsersFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return ignoredUsersFragment.ignoredUsersViewModelFactory.create(state)
        }
    }

    init {
        observeIgnoredUsers()
    }

    private fun observeIgnoredUsers() {
        session.rx()
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

        session.unIgnoreUserIds(listOf(action.userId), object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            unIgnoreRequest = Fail(failure)
                    )
                }

                _viewEvents.post(IgnoredUsersViewEvents.Failure(failure))
            }

            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            unIgnoreRequest = Success(data)
                    )
                }
            }
        })
    }
}
