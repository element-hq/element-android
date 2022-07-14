/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.voice

import android.content.Context
import android.os.Build
import im.vector.app.features.VectorFeatures
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class VoiceRecorderProvider @Inject constructor(
        private val context: Context,
        private val vectorFeatures: VectorFeatures,
) {
    fun provideVoiceRecorder(): VoiceRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vectorFeatures.forceUsageOfOpusEncoder().not()) {
            VoiceRecorderQ(context)
        } else {
            VoiceRecorderL(context, Dispatchers.IO)
        }
    }
}
