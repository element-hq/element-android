/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast

import android.media.MediaPlayer

sealed class VoiceBroadcastFailure : Throwable() {
    sealed class RecordingError : VoiceBroadcastFailure() {
        object NoPermission : RecordingError()
        object BlockedBySomeoneElse : RecordingError()
        object UserAlreadyBroadcasting : RecordingError()
    }

    sealed class ListeningError : VoiceBroadcastFailure() {
        /**
         * @property what the type of error that has occurred, see [MediaPlayer.OnErrorListener.onError].
         * @property extra an extra code, specific to the error, see [MediaPlayer.OnErrorListener.onError].
         */
        data class UnableToPlay(val what: Int, val extra: Int) : ListeningError()
        data class PrepareMediaPlayerError(override val cause: Throwable? = null) : ListeningError()
        object UnableToDecrypt : ListeningError()
    }
}
