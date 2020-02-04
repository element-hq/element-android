/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.riotx.features.attachments.preview

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.riotx.core.platform.VectorViewModel

class AttachmentsPreviewViewModel @AssistedInject constructor(@Assisted initialState: AttachmentsPreviewViewState)
    : VectorViewModel<AttachmentsPreviewViewState, AttachmentsPreviewAction, AttachmentsPreviewViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: AttachmentsPreviewViewState): AttachmentsPreviewViewModel
    }

    companion object : MvRxViewModelFactory<AttachmentsPreviewViewModel, AttachmentsPreviewViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: AttachmentsPreviewViewState): AttachmentsPreviewViewModel? {
            val fragment: AttachmentsPreviewFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    override fun handle(action: AttachmentsPreviewAction) {
        //TODO
    }
}
