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

package im.vector.app.features.call.conference

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitsiActiveConferenceHolder @Inject constructor(context: Context) {

    private var activeConference: String? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(ConferenceEventObserver(context, this::onBroadcastEvent))
    }

    fun isJoined(confId: String?): Boolean {
        return confId != null && activeConference?.endsWith(confId).orFalse()
    }

    private fun onBroadcastEvent(conferenceEvent: ConferenceEvent) {
        when (conferenceEvent) {
            is ConferenceEvent.Joined     -> activeConference = conferenceEvent.extractConferenceUrl()
            is ConferenceEvent.Terminated -> activeConference = null
            else                          -> Unit
        }
    }
}
