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

package im.vector.riotx.features.settings.ignored

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel

data class IgnoredUsersViewState(
        val ignoredUserIds: List<String> = emptyList(),
        val unIgnoreRequest: Async<Unit> = Uninitialized
) : MvRxState


sealed class IgnoredUsersAction {
    data class UnIgnore(val userId: String) : IgnoredUsersAction()
}

class IgnoredUsersViewModel @AssistedInject constructor(@Assisted initialState: IgnoredUsersViewState,
                                                        private val session: Session) : VectorViewModel<IgnoredUsersViewState>(initialState) {

    @AssistedInject.Factory
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
                .liveIgnoredUserIds()
                .execute { async ->
                    copy(
                            ignoredUserIds = async.invoke().orEmpty()
                    )
                }
    }

    fun handle(action: IgnoredUsersAction) {
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

                _requestErrorLiveData.postLiveEvent(failure)
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
