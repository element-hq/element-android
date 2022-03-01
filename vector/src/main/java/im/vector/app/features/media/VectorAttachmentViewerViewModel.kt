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

package im.vector.app.features.media

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.media.domain.usecase.DownloadMediaUseCase
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class VectorAttachmentViewerViewModel @AssistedInject constructor(
        @Assisted initialState: VectorDummyViewState,
        private val session: Session,
        private val downloadMediaUseCase: DownloadMediaUseCase
) : VectorViewModel<VectorDummyViewState, VectorAttachmentViewerAction, VectorAttachmentViewerViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VectorAttachmentViewerViewModel, VectorDummyViewState> {
        override fun create(initialState: VectorDummyViewState): VectorAttachmentViewerViewModel
    }

    companion object : MavericksViewModelFactory<VectorAttachmentViewerViewModel, VectorDummyViewState> by hiltMavericksViewModelFactory()

    var pendingAction: VectorAttachmentViewerAction? = null

    override fun handle(action: VectorAttachmentViewerAction) {
        when (action) {
            is VectorAttachmentViewerAction.DownloadMedia -> handleDownloadAction(action)
        }
    }

    private fun handleDownloadAction(action: VectorAttachmentViewerAction.DownloadMedia) {
        // launch in the coroutine scope session to avoid binding the coroutine to the lifecycle of the VM
        session.coroutineScope.launch {
            // Success event is handled via a notification inside the use case
            downloadMediaUseCase.execute(action.file)
                    .onFailure { _viewEvents.post(VectorAttachmentViewerViewEvents.ErrorDownloadingMedia(it)) }
        }
    }
}
