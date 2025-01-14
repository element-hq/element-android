/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.ignored

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.flow

class IgnoredUsersViewModel @AssistedInject constructor(
        @Assisted initialState: IgnoredUsersViewState,
        private val session: Session
) : VectorViewModel<IgnoredUsersViewState, IgnoredUsersAction, IgnoredUsersViewEvents>(initialState) {

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
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            val viewEvent = try {
                session.userService().unIgnoreUserIds(listOf(action.userId))
                IgnoredUsersViewEvents.Success
            } catch (throwable: Throwable) {
                IgnoredUsersViewEvents.Failure(throwable)
            }
            setState { copy(isLoading = false) }
            _viewEvents.post(viewEvent)
        }
    }
}
