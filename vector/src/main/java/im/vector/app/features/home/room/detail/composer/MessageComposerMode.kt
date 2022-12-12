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

package im.vector.app.features.home.room.detail.composer

import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

sealed interface MessageComposerMode {
    data class Normal(val content: CharSequence?) : MessageComposerMode

    sealed class Special(open val event: TimelineEvent, open val defaultContent: CharSequence) : MessageComposerMode
    data class Edit(override val event: TimelineEvent, override val defaultContent: CharSequence) : Special(event, defaultContent)
    data class Quote(override val event: TimelineEvent, override val defaultContent: CharSequence) : Special(event, defaultContent)
    data class Reply(override val event: TimelineEvent, override val defaultContent: CharSequence) : Special(event, defaultContent)
}
