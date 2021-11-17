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
package im.vector.app.features.roomprofile.alias.detail

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session

class RoomAliasBottomSheetViewModel @AssistedInject constructor(
        @Assisted initialState: RoomAliasBottomSheetState,
        session: Session
) : VectorViewModel<RoomAliasBottomSheetState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomAliasBottomSheetViewModel, RoomAliasBottomSheetState> {
        override fun create(initialState: RoomAliasBottomSheetState): RoomAliasBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<RoomAliasBottomSheetViewModel, RoomAliasBottomSheetState> by hiltMavericksViewModelFactory()

    init {
        setState {
            copy(
                    matrixToLink = session.permalinkService().createPermalink(alias)
            )
        }
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
