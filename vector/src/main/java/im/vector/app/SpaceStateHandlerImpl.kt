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

import androidx.lifecycle.LifecycleOwner
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
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles the global app state.
 * It is required that this class is added as an observer to ProcessLifecycleOwner.get().lifecycle in [VectorApplication]
 */
@Singleton
class SpaceStateHandlerImpl @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val uiStateRepository: UiStateRepository,
        private val activeSessionHolder: ActiveSessionHolder,
        private val analyticsTracker: AnalyticsTracker
) : SpaceStateHandler {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val selectedSpaceDataSource = BehaviorDataSource<Option<RoomSummary>>(Option.empty())
    private val selectedSpaceFlow = selectedSpaceDataSource.stream()
    private val spaceBackstack = ArrayDeque<String?>()

    override fun getCurrentSpace(): RoomSummary? {
        return selectedSpaceDataSource.currentValue?.orNull()?.let { spaceSummary ->
            activeSessionHolder.getSafeActiveSession()?.roomService()?.getRoomSummary(spaceSummary.roomId)
        }
    }

    override fun setCurrentSpace(
            spaceId: String?,
            session: Session?,
            persistNow: Boolean,
            isForwardNavigation: Boolean,
    ) {
        val activeSession = session ?: activeSessionHolder.getSafeActiveSession() ?: return
        val currentSpace = selectedSpaceDataSource.currentValue?.orNull()
        val spaceSummary = spaceId?.let { activeSession.getRoomSummary(spaceId) }
        val sameSpaceSelected = currentSpace != null && spaceId == currentSpace.roomId

        if (sameSpaceSelected) {
            return
        }

        if (isForwardNavigation) {
            spaceBackstack.addLast(currentSpace?.roomId)
        }

        if (persistNow) {
            uiStateRepository.storeSelectedSpace(spaceSummary?.roomId, activeSession.sessionId)
        }

        if (spaceSummary == null) {
            selectedSpaceDataSource.post(Option.empty())
        } else {
            selectedSpaceDataSource.post(Option.just(spaceSummary))
        }

        if (spaceId != null) {
            activeSession.coroutineScope.launch(Dispatchers.IO) {
                tryOrNull {
                    activeSession.getRoom(spaceId)?.membershipService()?.loadRoomMembersIfNeeded()
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
                        setCurrentSpace(uiStateRepository.getSelectedSpace(session.sessionId), session)
                        observeSyncStatus(session)
                    }
                }
                .launchIn(coroutineScope)
    }

    private fun observeSyncStatus(session: Session) {
        session.syncService().getSyncRequestStateFlow()
                .filterIsInstance<SyncRequestState.IncrementalSyncDone>()
                .map { session.spaceService().getRootSpaceSummaries().size }
                .distinctUntilChanged()
                .onEach { spacesNumber ->
                    analyticsTracker.updateUserProperties(UserProperties(numSpaces = spacesNumber))
                }.launchIn(session.coroutineScope)
    }

    override fun getSpaceBackstack() = spaceBackstack

    override fun getSelectedSpaceFlow() = selectedSpaceFlow

    override fun getSafeActiveSpaceId(): String? {
        return selectedSpaceDataSource.currentValue?.orNull()?.roomId
    }

    override fun onResume(owner: LifecycleOwner) {
        observeActiveSession()
    }

    override fun onPause(owner: LifecycleOwner) {
        coroutineScope.coroutineContext.cancelChildren()
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        uiStateRepository.storeSelectedSpace(selectedSpaceDataSource.currentValue?.orNull()?.roomId, session.sessionId)
    }
}
