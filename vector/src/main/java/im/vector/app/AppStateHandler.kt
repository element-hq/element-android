/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app

import androidx.lifecycle.DefaultLifecycleObserver
import arrow.core.Option
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles the global app state.
 * It is required that this class is added as an observer to ProcessLifecycleOwner.get().lifecycle in [VectorApplication]
 */
@Singleton
class AppStateHandler @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val uiStateRepository: UiStateRepository,
        private val activeSessionHolder: ActiveSessionHolder,
        private val analyticsTracker: AnalyticsTracker
) : DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val selectedSpaceDataSource = BehaviorDataSource<Option<RoomSummary>>(Option.empty())

    val selectedSpaceFlow = selectedSpaceDataSource.stream()

    private val spaceBackstack = ArrayDeque<String?>()

    fun getCurrentSpace(): RoomSummary? {
        return selectedSpaceDataSource.currentValue?.orNull()?.let { spaceSummary ->
            activeSessionHolder.getSafeActiveSession()?.roomService()?.getRoomSummary(spaceSummary.roomId)
        }
    }

    fun setCurrentSpace(
            spaceId: String?,
            session: Session? = null,
            persistNow: Boolean = false,
            isForwardNavigation: Boolean = true,
    ) {
        val activeSession = session ?: activeSessionHolder.getSafeActiveSession() ?: return
        val currentSpace = selectedSpaceDataSource.currentValue?.orNull()
        val spaceSummary = spaceId?.let { activeSession.getRoomSummary(spaceId) }
        val sameSpaceSelected = currentSpace != null && spaceId == currentSpace.roomId

        if (sameSpaceSelected) {
            return
        }

interface AppStateHandler : DefaultLifecycleObserver {

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

    fun getSelectedSpaceFlow(): Flow<Option<RoomSummary>>

    fun getSafeActiveSpaceId(): String?
}
