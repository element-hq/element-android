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
import androidx.lifecycle.LifecycleObserver

sealed class ConferenceEvent(open val data: Map<String, Any>) {
    data class Terminated(override val data: Map<String, Any>) : ConferenceEvent(data)
    data class WillJoin(override val data: Map<String, Any>) : ConferenceEvent(data)
    data class Joined(override val data: Map<String, Any>) : ConferenceEvent(data)

    fun extractConferenceUrl(): String? {
        return null
    }
}

class ConferenceEventEmitter(private val context: Context)  { fun emitConferenceEnded() {} }

class ConferenceEventObserver(private val context: Context, private val onBroadcastEvent: (ConferenceEvent) -> Unit) : LifecycleObserver
