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

package im.vector.app.features.home.room.detail.timeline.helper

import android.os.Handler
import android.os.Looper
import im.vector.app.core.di.ScreenScope
import javax.inject.Inject

@ScreenScope
class VoiceMessagePlaybackTracker @Inject constructor() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableMapOf<String, Listener>()
    private val states = mutableMapOf<String, Listener.State>()

    fun track(id: String, listener: Listener) {
        listeners[id] = listener

        val currentState = states[id] ?: Listener.State.Idle(0)
        mainHandler.post {
            listener.onUpdate(currentState)
        }
    }

    fun makeAllPlaybacksIdle() {
        listeners.keys.forEach { key ->
            val currentPlaybackTime = getPlaybackTime(key)
            states[key] = Listener.State.Idle(currentPlaybackTime)
            mainHandler.post {
                listeners[key]?.onUpdate(Listener.State.Idle(currentPlaybackTime))
            }
        }
    }

    fun startPlayback(id: String) {
        val currentPlaybackTime = getPlaybackTime(id)
        val currentState = Listener.State.Playing(currentPlaybackTime)
        states[id] = currentState
        mainHandler.post {
            listeners[id]?.onUpdate(currentState)
        }
        // Make active playback IDLE
        states
                .filter { it.key != id }
                .filter { it.value is Listener.State.Playing }
                .keys
                .forEach { key ->
                    val playbackTime = getPlaybackTime(key)
                    val state = Listener.State.Idle(playbackTime)
                    states[key] = state
                    mainHandler.post {
                        listeners[key]?.onUpdate(state)
                    }
                }
    }

    fun stopPlayback(id: String, rememberPlaybackTime: Boolean = true) {
        val currentPlaybackTime = if (rememberPlaybackTime) getPlaybackTime(id) else 0
        states[id] = Listener.State.Idle(currentPlaybackTime)
        mainHandler.post {
            listeners[id]?.onUpdate(states[id]!!)
        }
    }

    fun updateCurrentPlaybackTime(id: String, time: Int) {
        states[id] = Listener.State.Playing(time)
        mainHandler.post {
            listeners[id]?.onUpdate(states[id]!!)
        }
    }

    fun updateCurrentRecording(id: String, amplitudeList: List<Int>) {
        states[id] = Listener.State.Recording(amplitudeList)
        mainHandler.post {
            listeners[id]?.onUpdate(states[id]!!)
        }
    }

    fun getPlaybackState(id: String) = states[id]

    fun getPlaybackTime(id: String): Int {
        return when (val state = states[id]) {
            is Listener.State.Playing    -> state.playbackTime
            is Listener.State.Idle       -> state.playbackTime
            else                         -> 0
        }
    }

    fun clear() {
        listeners.forEach {
            it.value.onUpdate(Listener.State.Idle(0))
        }
        listeners.clear()
        states.clear()
    }

    companion object {
        var RECORDING_ID = "RECORDING_ID"
    }

    interface Listener {

        fun onUpdate(state: State)

        sealed class State {
            data class Idle(val playbackTime: Int): State()
            data class Playing(val playbackTime: Int) : State()
            data class Recording(val amplitudeList: List<Int>) : State()
        }
    }
}
