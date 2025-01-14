/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

interface AttachmentInteractionListener {
    fun onDismiss()
    fun onShare()
    fun onDownload()
    fun onPlayPause(play: Boolean)
    fun videoSeekTo(percent: Int)
}
