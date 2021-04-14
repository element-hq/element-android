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
import arrow.core.Option
import im.vector.app.core.utils.BehaviorDataSource
import im.vector.app.features.ui.UiStateRepository
import io.reactivex.disposables.CompositeDisposable
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class handles the global app state.
 * It requires to be added to ProcessLifecycleOwner.get().lifecycle
 */
// TODO Keep this class for now, will maybe be used fro Space
@Singleton
class AppStateHandler @Inject constructor(
        sessionDataSource: ActiveSessionDataSource,
        private val uiStateRepository: UiStateRepository
) : LifecycleObserver {

    private val compositeDisposable = CompositeDisposable()

    val selectedSpaceDataSource = BehaviorDataSource<Option<RoomSummary>>(Option.empty())

    init {
        // restore current space from ui state
        sessionDataSource.currentValue?.orNull()?.let { session ->
            uiStateRepository.getSelectedSpace(session.sessionId)?.let { selectedSpaceId ->
                session.getRoomSummary(selectedSpaceId)?.let {
                    selectedSpaceDataSource.post(Option.just(it))
                }
            }
        }
    }

    fun safeActiveSpaceId() : String? {
        return selectedSpaceDataSource.currentValue?.orNull()?.roomId?.takeIf {
            MatrixPatterns.isRoomId(it)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        compositeDisposable.clear()
    }
}
