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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.utils.BehaviorDataSource
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.UiStateRepository
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import timber.log.Timber
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
        sessionDataSource: ActiveSessionDataSource,
        private val uiStateRepository: UiStateRepository,
        private val activeSessionHolder: ActiveSessionHolder,
        vectorPreferences: VectorPreferences
) : LifecycleObserver {

    private val compositeDisposable = CompositeDisposable()

    private val selectedSpaceDataSource = BehaviorDataSource(
            // TODO get that from latest persisted?
            if (vectorPreferences.labSpaces())
                RoomGroupingMethod.BySpace(null)
            else RoomGroupingMethod.ByLegacyGroup(null)
    )

    val selectedRoomGroupingObservable = selectedSpaceDataSource.observe()

    fun getCurrentRoomGroupingMethod(): RoomGroupingMethod = selectedSpaceDataSource.currentValue ?: RoomGroupingMethod.BySpace(null)

    fun setCurrentSpace(spaceId: String?, session: Session? = null) {
        val uSession = session ?: activeSessionHolder.getSafeActiveSession()
        if (selectedSpaceDataSource.currentValue is RoomGroupingMethod.BySpace
                && spaceId == selectedSpaceDataSource.currentValue?.space()?.roomId) return
        val spaceSum = spaceId?.let { uSession?.getRoomSummary(spaceId) }
        selectedSpaceDataSource.post(RoomGroupingMethod.BySpace(spaceSum))
        if (spaceId != null) {
            GlobalScope.launch {
                tryOrNull {
                    uSession?.getRoom(spaceId)?.loadRoomMembersIfNeeded()
                }
            }
        }
    }

    fun setCurrentGroup(groupId: String?, session: Session? = null) {
        val uSession = session ?: activeSessionHolder.getSafeActiveSession()
        if (selectedSpaceDataSource.currentValue is RoomGroupingMethod.ByLegacyGroup
                && groupId == selectedSpaceDataSource.currentValue?.group()?.groupId) return
        val activeGroup = groupId?.let { uSession?.getGroupSummary(groupId) }
        selectedSpaceDataSource.post(RoomGroupingMethod.ByLegacyGroup(activeGroup))
        if (groupId != null) {
            GlobalScope.launch {
                tryOrNull {
                    uSession?.getGroup(groupId)?.fetchGroupData()
                }
            }
        }
    }

    init {

        sessionDataSource.observe()
                .distinctUntilChanged()
                .subscribe {
                    it.orNull()?.let { session ->
                        Timber.w("VAL: Latest method is space? ${uiStateRepository.isGroupingMethodSpace(session.sessionId)}")
                        if (uiStateRepository.isGroupingMethodSpace(session.sessionId)) {
                            uiStateRepository.getSelectedSpace(session.sessionId)?.let { selectedSpaceId ->
                                Timber.w("VAL: Latest selected space: $selectedSpaceId")
                                setCurrentSpace(selectedSpaceId, session)
                            }
                        } else {
                            uiStateRepository.getSelectedGroup(session.sessionId)?.let { selectedGroupId ->
                                setCurrentGroup(selectedGroupId, session)
                            }
                        }
                    }
        }.also {
            compositeDisposable.add(it)
        }
        // restore current space from ui state
    }

    fun safeActiveSpaceId(): String? {
        return (selectedSpaceDataSource.currentValue as? RoomGroupingMethod.BySpace)?.spaceSummary?.roomId
    }

    fun safeActiveGroupId(): String? {
        return (selectedSpaceDataSource.currentValue as? RoomGroupingMethod.ByLegacyGroup)?.groupSummary?.groupId
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        compositeDisposable.clear()
        Timber.w("VAL: entersBackground session: ${ activeSessionHolder.getSafeActiveSession()?.myUserId}")
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        when (val currentMethod = selectedSpaceDataSource.currentValue ?: RoomGroupingMethod.BySpace(null)) {
            is RoomGroupingMethod.BySpace -> {
                uiStateRepository.storeGroupingMethod(true, session)
                Timber.w("VAL: Store selected space: ${currentMethod.spaceSummary?.roomId}")
                uiStateRepository.storeSelectedSpace(currentMethod.spaceSummary?.roomId, session)
            }
            is RoomGroupingMethod.ByLegacyGroup -> {
                uiStateRepository.storeGroupingMethod(false, session)
                Timber.w("VAL: Store group space: ${currentMethod.groupSummary?.groupId}")
                uiStateRepository.storeSelectedGroup(currentMethod.groupSummary?.groupId, session)
            }
        }
    }
}
