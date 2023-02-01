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
