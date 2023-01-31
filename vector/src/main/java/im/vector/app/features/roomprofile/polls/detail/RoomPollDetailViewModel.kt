/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.detail

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel

class RoomPollDetailViewModel @AssistedInject constructor(
        @Assisted initialState: RoomPollDetailViewState,
) : VectorViewModel<RoomPollDetailViewState, RoomPollDetailAction, RoomPollDetailViewEvent>(initialState) {

    init {
        // Subscribe to the poll event and map it
    }

    override fun handle(action: RoomPollDetailAction) {

    }
}
