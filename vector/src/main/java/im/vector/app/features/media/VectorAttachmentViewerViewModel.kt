/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright (C) 2018 stfalcon.com
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

package im.vector.app.features.media

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.platform.VectorViewModel

class VectorAttachmentViewerViewModel @AssistedInject constructor(
        @Assisted initialState: VectorDummyViewState
) : VectorViewModel<VectorDummyViewState, VectorAttachmentViewerAction, VectorAttachmentViewerViewEvents>(initialState) {

    /* ==========================================================================================
     * Factory
     * ========================================================================================== */

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VectorAttachmentViewerViewModel, VectorDummyViewState> {
        override fun create(initialState: VectorDummyViewState): VectorAttachmentViewerViewModel
    }

    /* ==========================================================================================
     * Specialization
     * ========================================================================================== */

    override fun handle(action: VectorAttachmentViewerAction) {
        TODO("Not yet implemented")
    }
}
