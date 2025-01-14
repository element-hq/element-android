/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.CreatedRoom
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

class SpacePeopleViewModel @AssistedInject constructor(
        @Assisted val initialState: SpacePeopleViewState,
        private val rawService: RawService,
        private val session: Session,
        private val analyticsTracker: AnalyticsTracker
) : VectorViewModel<SpacePeopleViewState, SpacePeopleViewAction, SpacePeopleViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpacePeopleViewModel, SpacePeopleViewState> {
        override fun create(initialState: SpacePeopleViewState): SpacePeopleViewModel
    }

    companion object : MavericksViewModelFactory<SpacePeopleViewModel, SpacePeopleViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: SpacePeopleViewAction) {
        when (action) {
            is SpacePeopleViewAction.ChatWith -> handleChatWith(action)
            SpacePeopleViewAction.InviteToSpace -> handleInviteToSpace()
        }
    }

    private fun handleInviteToSpace() {
        _viewEvents.post(SpacePeopleViewEvents.InviteToSpace(initialState.spaceId))
    }

    private fun handleChatWith(action: SpacePeopleViewAction.ChatWith) {
        val otherUserId = action.member.userId
        if (otherUserId == session.myUserId) return
        val existingRoomId = session.roomService().getExistingDirectRoomWithUser(otherUserId)
        if (existingRoomId != null) {
            // just open it
            _viewEvents.post(SpacePeopleViewEvents.OpenRoom(existingRoomId))
            return
        }
        setState { copy(createAndInviteState = Loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val adminE2EByDefault = rawService.getElementWellknown(session.sessionParams)
                    ?.isE2EByDefault()
                    ?: true

            val roomParams = CreateRoomParams()
                    .apply {
                        invitedUserIds.add(otherUserId)
                        setDirectMessage()
                        enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
                    }

            try {
                val roomId = session.roomService().createRoom(roomParams)
                analyticsTracker.capture(CreatedRoom(isDM = roomParams.isDirect.orFalse()))
                _viewEvents.post(SpacePeopleViewEvents.OpenRoom(roomId))
                setState { copy(createAndInviteState = Success(roomId)) }
            } catch (failure: Throwable) {
                setState { copy(createAndInviteState = Fail(failure)) }
            }
        }
    }
}
