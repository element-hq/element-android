/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.send

import com.nikitakozlov.pury.Pury

private const val ROOT_STAGE = "ROOT_STAGE"

object SendPerformanceTracker {

    enum class Stage(val displayName: String, val index: Int) {
        LOCAL_ECHO("Local echo", 1),
        ENCRYPT_WORKER("Encrypt", LOCAL_ECHO.index + 1),
        SEND_WORKER("Send worker", ENCRYPT_WORKER.index + 1),
    }

    private val currentKeys = ArrayList<String>()
    private val currentStages = HashMap<String, Stage>()

    fun startProfiling(eventId: String) {
        if (currentKeys.contains(eventId)) {
            return
        }
        currentKeys.add(eventId)
        val profilerName = profilerName(eventId)
        Pury.startProfiling(profilerName, ROOT_STAGE, 0, 1)
    }

    fun stopProfiling(eventId: String) {
        if (!currentKeys.contains(eventId)) {
            return
        }
        Pury.stopProfiling(profilerName(eventId), ROOT_STAGE, 1)
        currentKeys.remove(eventId)
    }

    private fun profilerName(eventId: String) = "SEND_PROFILER_$eventId"

    fun startStage(eventId: String, newStage: Stage) {
        if (!currentKeys.contains(eventId)) return
        Pury.startProfiling(profilerName(eventId), newStage.displayName, newStage.index, 1)
    }

    fun stopStage(eventId: String, stage: Stage) {
        Pury.stopProfiling(profilerName(eventId), stage.displayName, 1)
    }
}
