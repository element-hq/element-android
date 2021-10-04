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

package im.vector.app.features.home.room.detail.timeline.url

import org.matrix.android.sdk.api.session.media.PreviewUrlData

/**
 * The state representing a preview url UI state for an Event
 */
sealed class PreviewUrlUiState {
    // No info
    object Unknown : PreviewUrlUiState()

    // The event does not contain any URLs
    object NoUrl : PreviewUrlUiState()

    // Loading
    object Loading : PreviewUrlUiState()

    // Error
    data class Error(val throwable: Throwable) : PreviewUrlUiState()

    // PreviewUrl data
    data class Data(val eventId: String,
                    val url: String,
                    val previewUrlData: PreviewUrlData) : PreviewUrlUiState()
}
