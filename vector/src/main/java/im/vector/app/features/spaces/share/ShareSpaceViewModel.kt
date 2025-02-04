/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.share

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

class ShareSpaceViewModel @AssistedInject constructor(
        @Assisted private val initialState: ShareSpaceViewState,
        private val session: Session
) : VectorViewModel<ShareSpaceViewState, ShareSpaceAction, ShareSpaceViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ShareSpaceViewModel, ShareSpaceViewState> {
        override fun create(initialState: ShareSpaceViewState): ShareSpaceViewModel
    }

    companion object : MavericksViewModelFactory<ShareSpaceViewModel, ShareSpaceViewState> by hiltMavericksViewModelFactory()

    init {
        val roomSummary = session.getRoomSummary(initialState.spaceId)
        setState {
            copy(
                    spaceSummary = roomSummary?.let { Success(it) } ?: Uninitialized,
                    canShareLink = roomSummary?.isPublic.orFalse()
            )
        }
        observePowerLevel()
    }

    private fun observePowerLevel() {
        val room = session.getRoom(initialState.spaceId) ?: return
        PowerLevelsFlowFactory(room)
                .createFlow()
                .onEach { powerLevelContent ->
                    val powerLevelsHelper = PowerLevelsHelper(powerLevelContent)
                    setState {
                        copy(
                                canInviteByMxId = powerLevelsHelper.isUserAbleToInvite(session.myUserId)
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: ShareSpaceAction) {
        when (action) {
            ShareSpaceAction.InviteByLink -> {
                val roomSummary = session.getRoomSummary(initialState.spaceId)
                val alias = roomSummary?.canonicalAlias
                val permalink = if (alias != null) {
                    session.permalinkService().createPermalink(alias)
                } else {
                    session.permalinkService().createRoomPermalink(initialState.spaceId)
                }
                if (permalink != null) {
                    _viewEvents.post(ShareSpaceViewEvents.ShowInviteByLink(permalink, roomSummary?.name ?: ""))
                }
            }
            ShareSpaceAction.InviteByMxId -> {
                _viewEvents.post(ShareSpaceViewEvents.NavigateToInviteUser(initialState.spaceId))
            }
        }
    }
}
