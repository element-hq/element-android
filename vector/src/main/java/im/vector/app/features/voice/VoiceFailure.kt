/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

sealed class VoiceFailure(cause: Throwable? = null) : Throwable(cause = cause) {
    data class UnableToPlay(val throwable: Throwable) : VoiceFailure(throwable)
    data class UnableToRecord(val throwable: Throwable) : VoiceFailure(throwable)
    object VoiceBroadcastInProgress : VoiceFailure()
}
