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

package im.vector.app.features.spaces.manage

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session

class SpaceManageSharedViewModel @AssistedInject constructor(
        @Assisted initialState: SpaceManageViewState,
        private val session: Session
) : VectorViewModel<SpaceManageViewState, SpaceManagedSharedAction, SpaceManagedSharedViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceManageViewState): SpaceManageSharedViewModel
    }

    companion object : MvRxViewModelFactory<SpaceManageSharedViewModel, SpaceManageViewState> {
        override fun create(viewModelContext: ViewModelContext, state: SpaceManageViewState): SpaceManageSharedViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: SpaceManagedSharedAction) {
        when (action) {
            SpaceManagedSharedAction.HandleBack -> {
                // for now finish
                _viewEvents.post(SpaceManagedSharedViewEvents.Finish)
            }
            SpaceManagedSharedAction.HideLoading -> _viewEvents.post(SpaceManagedSharedViewEvents.HideLoading)
            SpaceManagedSharedAction.ShowLoading -> _viewEvents.post(SpaceManagedSharedViewEvents.ShowLoading)
            SpaceManagedSharedAction.CreateRoom ->  _viewEvents.post(SpaceManagedSharedViewEvents.NavigateToCreateRoom)
            SpaceManagedSharedAction.ManageRooms -> _viewEvents.post(SpaceManagedSharedViewEvents.NavigateToManageRooms)
            SpaceManagedSharedAction.OpenSpaceAliasesSettings -> _viewEvents.post(SpaceManagedSharedViewEvents.NavigateToAliasSettings)
        }
    }
}
