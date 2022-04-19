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

package im.vector.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import arrow.core.Option
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.utils.BehaviorDataSource
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.ui.UiStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject
import javax.inject.Singleton

sealed class RoomGroupingMethod {
    data class ByLegacyGroup(val groupSummary: GroupSummary?) : RoomGroupingMethod()
    data class BySpace(val spaceSummary: RoomSummary?) : RoomGroupingMethod()
}

fun RoomGroupingMethod.space() = (this as? RoomGroupingMethod.BySpace)?.spaceSummary
fun RoomGroupingMethod.group() = (this as? RoomGroupingMethod.ByLegacyGroup)?.groupSummary

/**
 * This class handles the global app state.
 * It requires to be added to ProcessLifecycleOwner.get().lifecycle
 */
// TODO Keep this class for now, will maybe be used fro Space
@Singleton
class AppStateHandler @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val uiStateRepository: UiStateRepository,
        private val activeSessionHolder: ActiveSessionHolder,
        private val analyticsTracker: AnalyticsTracker
) : DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val selectedSpaceDataSource = BehaviorDataSource<Option<RoomGroupingMethod>>(Option.empty())

    val selectedRoomGroupingFlow = selectedSpaceDataSource.stream()

    fun getCurrentRoomGroupingMethod(): RoomGroupingMethod? {
        // XXX we should somehow make it live :/ just a work around
        // For example just after creating a space and switching to it the
        // name in the app Bar could show Empty Room, and it will not update unless you
        // switch space
        return selectedSpaceDataSource.currentValue?.orNull()?.let {
            if (it is RoomGroupingMethod.BySpace) {
                // try to refresh sum?
                it.spaceSummary?.roomId?.let { activeSessionHolder.getSafeActiveSession()?.getRoomSummary(it) }?.let {
                    RoomGroupingMethod.BySpace(it)
                } ?: it
            } else it
        }
    }

    fun setCurrentSpace(spaceId: String?, session: Session? = null, persistNow: Boolean = false) {
        val uSession = session ?: activeSessionHolder.getSafeActiveSession() ?: return
        if (selectedSpaceDataSource.currentValue?.orNull() is RoomGroupingMethod.BySpace &&
                spaceId == selectedSpaceDataSource.currentValue?.orNull()?.space()?.roomId) return
        val spaceSum = spaceId?.let { uSession.getRoomSummary(spaceId) }

        if (persistNow) {
            uiStateRepository.storeGroupingMethod(true, uSession.sessionId)
            uiStateRepository.storeSelectedSpace(spaceSum?.roomId, uSession.sessionId)
        }

        selectedSpaceDataSource.post(Option.just(RoomGroupingMethod.BySpace(spaceSum)))
        if (spaceId != null) {
            uSession.coroutineScope.launch(Dispatchers.IO) {
                tryOrNull {
                    uSession.getRoom(spaceId)?.loadRoomMembersIfNeeded()
                }
            }
        }
    }

    fun setCurrentGroup(groupId: String?, session: Session? = null) {
        val uSession = session ?: activeSessionHolder.getSafeActiveSession() ?: return
        if (selectedSpaceDataSource.currentValue?.orNull() is RoomGroupingMethod.ByLegacyGroup &&
                groupId == selectedSpaceDataSource.currentValue?.orNull()?.group()?.groupId) return
        val activeGroup = groupId?.let { uSession.getGroupSummary(groupId) }
        selectedSpaceDataSource.post(Option.just(RoomGroupingMethod.ByLegacyGroup(activeGroup)))
        if (groupId != null) {
            uSession.coroutineScope.launch {
                tryOrNull {
                    uSession.getGroup(groupId)?.fetchGroupData()
                }
            }
        }
    }

    private fun observeActiveSession() {
        sessionDataSource.stream()
                .distinctUntilChanged()
                .onEach {
                    // sessionDataSource could already return a session while activeSession holder still returns null
                    it.orNull()?.let { session ->
                        if (uiStateRepository.isGroupingMethodSpace(session.sessionId)) {
                            setCurrentSpace(uiStateRepository.getSelectedSpace(session.sessionId), session)
                        } else {
                            setCurrentGroup(uiStateRepository.getSelectedGroup(session.sessionId), session)
                        }
                        observeSyncStatus(session)
                    }
                }
                .launchIn(coroutineScope)
    }

    private fun observeSyncStatus(session: Session) {
        session.getSyncStatusLive()
                .asFlow()
                .filterIsInstance<SyncStatusService.Status.IncrementalSyncDone>()
                .map { session.spaceService().getRootSpaceSummaries().size }
                .distinctUntilChanged()
                .onEach { spacesNumber ->
                    analyticsTracker.updateUserProperties(UserProperties(numSpaces = spacesNumber))
                }.launchIn(session.coroutineScope)
    }

    fun safeActiveSpaceId(): String? {
        return (selectedSpaceDataSource.currentValue?.orNull() as? RoomGroupingMethod.BySpace)?.spaceSummary?.roomId
    }

    fun safeActiveGroupId(): String? {
        return (selectedSpaceDataSource.currentValue?.orNull() as? RoomGroupingMethod.ByLegacyGroup)?.groupSummary?.groupId
    }

    override fun onResume(owner: LifecycleOwner) {
        observeActiveSession()
    }

    override fun onPause(owner: LifecycleOwner) {
        coroutineScope.coroutineContext.cancelChildren()
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        when (val currentMethod = selectedSpaceDataSource.currentValue?.orNull() ?: RoomGroupingMethod.BySpace(null)) {
            is RoomGroupingMethod.BySpace       -> {
                uiStateRepository.storeGroupingMethod(true, session.sessionId)
                uiStateRepository.storeSelectedSpace(currentMethod.spaceSummary?.roomId, session.sessionId)
            }
            is RoomGroupingMethod.ByLegacyGroup -> {
                uiStateRepository.storeGroupingMethod(false, session.sessionId)
                uiStateRepository.storeSelectedGroup(currentMethod.groupSummary?.groupId, session.sessionId)
            }
        }
    }
}
