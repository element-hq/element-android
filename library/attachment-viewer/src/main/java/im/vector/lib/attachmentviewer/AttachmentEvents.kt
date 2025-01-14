/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

sealed class AttachmentEvents {
    data class VideoEvent(val isPlaying: Boolean, val progress: Int, val duration: Int) : AttachmentEvents()
}

interface AttachmentEventListener {
    fun onEvent(event: AttachmentEvents)
}

sealed class AttachmentCommands {
    object PauseVideo : AttachmentCommands()
    object StartVideo : AttachmentCommands()
    data class SeekTo(val percentProgress: Int) : AttachmentCommands()
}
