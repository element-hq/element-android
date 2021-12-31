/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.upgrade

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class MigrateRoomViewModel @AssistedInject constructor(
        @Assisted initialState: MigrateRoomViewState,
        private val session: Session,
        private val upgradeRoomViewModelTask: UpgradeRoomViewModelTask) :
    VectorViewModel<MigrateRoomViewState, MigrateRoomAction, EmptyViewEvents>(initialState) {

    init {
        val room = session.getRoom(initialState.roomId)
        val summary = session.getRoomSummary(initialState.roomId)
        setState {
            copy(
                    currentVersion = room?.getRoomVersion(),
                    isPublic = summary?.isPublic ?: false,
                    otherMemberCount = summary?.otherMemberIds?.count() ?: 0,
                    knownParents = summary?.flattenParentIds ?: emptyList()
            )
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<MigrateRoomViewModel, MigrateRoomViewState> {
        override fun create(initialState: MigrateRoomViewState): MigrateRoomViewModel
    }

    companion object : MavericksViewModelFactory<MigrateRoomViewModel, MigrateRoomViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: MigrateRoomAction) {
        when (action) {
            is MigrateRoomAction.SetAutoInvite             -> {
                setState {
                    copy(shouldIssueInvites = action.autoInvite)
                }
            }
            is MigrateRoomAction.SetUpdateKnownParentSpace -> {
                setState {
                    copy(shouldUpdateKnownParents = action.update)
                }
            }
            MigrateRoomAction.UpgradeRoom                  -> {
                handleUpgradeRoom()
            }
        }
    }

    private fun handleUpgradeRoom() = withState { state ->
        val summary = session.getRoomSummary(state.roomId)
        setState {
            copy(upgradingStatus = Loading())
        }
        session.coroutineScope.launch {
            val userToInvite = if (state.autoMigrateMembersAndParents) {
                summary?.otherMemberIds?.takeIf { !state.isPublic }
            } else {
                summary?.otherMemberIds?.takeIf { state.shouldIssueInvites }
            }.orEmpty()

            val parentSpaceToUpdate = if (state.autoMigrateMembersAndParents) {
                summary?.flattenParentIds
            } else {
                summary?.flattenParentIds?.takeIf { state.shouldUpdateKnownParents }
            }.orEmpty()

            val result = upgradeRoomViewModelTask.execute(UpgradeRoomViewModelTask.Params(
                    roomId = state.roomId,
                    newVersion = state.newVersion,
                    userIdsToAutoInvite = userToInvite,
                    parentSpaceToUpdate = parentSpaceToUpdate,
                    progressReporter = { indeterminate, progress, total ->
                        setState {
                            copy(
                                    upgradingProgress = progress,
                                    upgradingProgressTotal = total,
                                    upgradingProgressIndeterminate = indeterminate
                            )
                        }
                    }
            ))

            setState {
                copy(upgradingStatus = Success(result))
            }
        }
    }
}
