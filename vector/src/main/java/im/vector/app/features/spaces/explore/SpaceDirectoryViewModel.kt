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

package im.vector.app.features.spaces.explore

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

data class SpaceDirectoryState(
        // The current filter
        val spaceId: String,
        val currentFilter: String = "",
        val summary: Async<RoomSummary> = Uninitialized,
        // True if more result are available server side
        val hasMore: Boolean = false,
        // Set of joined roomId / spaces,
        val joinedRoomsIds: Set<String> = emptySet()
) : MvRxState {
    constructor(args: SpaceDirectoryArgs) : this(spaceId = args.spaceId)
}

sealed class SpaceDirectoryViewAction : VectorViewModelAction

sealed class SpaceDirectoryViewEvents : VectorViewEvents

class SpaceDirectoryViewModel @AssistedInject constructor(
        @Assisted initialState: SpaceDirectoryState,
        private val session: Session
) : VectorViewModel<SpaceDirectoryState, VectorViewModelAction, SpaceDirectoryViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceDirectoryState): SpaceDirectoryViewModel
    }

    companion object : MvRxViewModelFactory<SpaceDirectoryViewModel, SpaceDirectoryState> {
        override fun create(viewModelContext: ViewModelContext, state: SpaceDirectoryState): SpaceDirectoryViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        val queryParams = roomSummaryQueryParams {
            roomId = QueryStringValue.Equals(initialState.spaceId)
        }

        viewModelScope.launch(Dispatchers.IO) {
            session
                    .rx()
                    .liveSpaceSummaries(queryParams)
                    .observeOn(Schedulers.computation())
                    .map { sum -> Optional.from(sum.firstOrNull()) }
                    .unwrap()
                    .execute { async ->
                        copy(summary = async)
                    }
        }
    }

    override fun handle(action: VectorViewModelAction) {
        TODO("Not yet implemented")
    }
}
