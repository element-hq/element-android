/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.spaces.preview

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.space.SpaceService
import org.matrix.android.sdk.internal.session.space.peeking.SpacePeekResult

class SpacePreviewViewModel @AssistedInject constructor(
        @Assisted private val initialState: SpacePreviewState,
        private val session: Session
) : VectorViewModel<SpacePreviewState, SpacePreviewViewAction, SpacePreviewViewEvents>(initialState) {

    private var initialized = false

    init {
        // do we have some things in cache?
        session.getRoomSummary(initialState.idOrAlias)?.let {
            setState {
                copy(name = it.name, avatarUrl = it.avatarUrl)
            }
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SpacePreviewState): SpacePreviewViewModel
    }

    companion object : MvRxViewModelFactory<SpacePreviewViewModel, SpacePreviewState> {
        override fun create(viewModelContext: ViewModelContext, state: SpacePreviewState): SpacePreviewViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: SpacePreviewViewAction) {
        when (action) {
            SpacePreviewViewAction.ViewReady -> handleReady()
            SpacePreviewViewAction.AcceptInvite -> handleAcceptInvite()
            SpacePreviewViewAction.DismissInvite -> handleDismissInvite()
        }
    }

    private fun handleDismissInvite() {
        TODO("Not yet implemented")
    }

    private fun handleAcceptInvite() = withState { state ->
        // Here we need to join the space himself as well as the default rooms in that space
        val spaceInfo = state.peekResult.invoke() as? SpacePeekResult.Success

        // TODO if we have no summary, we cannot find auto join rooms...
        // So maybe we should trigger a retry on summary after the join?
        val spaceVia = (spaceInfo?.summary?.roomPeekResult as? PeekResult.Success)?.viaServers ?: emptyList()
        val autoJoinChildren = spaceInfo?.summary?.children
                ?.filter { it.default == true }
                ?.map {
                    SpaceService.ChildAutoJoinInfo(
                            it.id,
                            // via servers
                            (it.roomPeekResult as? PeekResult.Success)?.viaServers ?: emptyList()
                    )
                } ?: emptyList()

        // trigger modal loading
        _viewEvents.post(SpacePreviewViewEvents.StartJoining)
        viewModelScope.launch(Dispatchers.IO) {
            val joinResult = session.spaceService().joinSpace(spaceInfo?.summary?.idOrAlias ?: initialState.idOrAlias, null, spaceVia, autoJoinChildren)
            when (joinResult) {
                SpaceService.JoinSpaceResult.Success,
                is SpaceService.JoinSpaceResult.PartialSuccess -> {
                    // For now we don't handle partial success, it's just success
                    _viewEvents.post(SpacePreviewViewEvents.JoinSuccess)
                }
                is SpaceService.JoinSpaceResult.Fail -> {
                    _viewEvents.post(SpacePreviewViewEvents.JoinFailure(joinResult.error?.toString()))
                }
            }
        }
    }

    private fun handleReady() {
        if (!initialized) {
            initialized = true
            // peek for the room
            setState {
                copy(peekResult = Loading())
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = session.spaceService().peekSpace(initialState.idOrAlias)
                    setState {
                        copy(peekResult = Success(result))
                    }
                } catch (failure: Throwable) {
                    setState {
                        copy(peekResult = Fail(failure))
                    }
                }
            }
        }
    }
}
