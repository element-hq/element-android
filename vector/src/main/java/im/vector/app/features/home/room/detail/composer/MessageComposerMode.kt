/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
