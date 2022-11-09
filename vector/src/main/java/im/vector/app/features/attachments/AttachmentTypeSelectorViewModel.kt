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

package im.vector.app.features.attachments

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.VectorFeatures
import im.vector.app.features.settings.VectorPreferences

class AttachmentTypeSelectorViewModel @AssistedInject constructor(
        @Assisted initialState: AttachmentTypeSelectorViewState,
        private val vectorFeatures: VectorFeatures,
        private val vectorPreferences: VectorPreferences,
) : VectorViewModel<AttachmentTypeSelectorViewState, AttachmentTypeSelectorAction, EmptyViewEvents>(initialState) {
    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AttachmentTypeSelectorViewModel, AttachmentTypeSelectorViewState> {
        override fun create(initialState: AttachmentTypeSelectorViewState): AttachmentTypeSelectorViewModel
    }

    companion object : MavericksViewModelFactory<AttachmentTypeSelectorViewModel, AttachmentTypeSelectorViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: AttachmentTypeSelectorAction) = when (action) {
        is AttachmentTypeSelectorAction.ToggleTextFormatting -> setTextFormattingEnabled(action.isEnabled)
    }

    init {
        setState {
            copy(
                    isLocationVisible = vectorFeatures.isLocationSharingEnabled(),
                    isVoiceBroadcastVisible = vectorFeatures.isVoiceBroadcastEnabled(),
                    isTextFormattingEnabled = vectorPreferences.isTextFormattingEnabled(),
            )
        }
    }

    private fun setTextFormattingEnabled(isEnabled: Boolean) {
        vectorPreferences.setTextFormattingEnabled(isEnabled)
        setState {
            copy(
                    isTextFormattingEnabled = isEnabled
            )
        }
    }
}

data class AttachmentTypeSelectorViewState(
        val isLocationVisible: Boolean = false,
        val isVoiceBroadcastVisible: Boolean = false,
        val isTextFormattingEnabled: Boolean = false,
) : MavericksState

sealed interface AttachmentTypeSelectorAction : VectorViewModelAction {
    data class ToggleTextFormatting(val isEnabled: Boolean) : AttachmentTypeSelectorAction
}
