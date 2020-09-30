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

package org.matrix.android.sdk.api.session.room.send

import com.nikitakozlov.pury.Pury

private const val ROOT_STAGE = "ROOT_STAGE"

object SendPerformanceTracker {

    enum class Stage() {
        LOCAL_ECHO,
        ENCRYPT_WORKER,
        ENCRYPT_GET_USERS,
        ENCRYPT_SET_ROOM_ENCRYPTION,
        ENCRYPT_MEGOLM_SHARE_KEYS,
        ENCRYPT_MEGOLM_ENCRYPT,
        SEND_WORKER,
        SEND_REQUEST
    }

    private val currentProfilers = ArrayList<String>()

    fun startProfiling(eventId: String?) {
        if (eventId == null) return
        if (currentProfilers.contains(eventId)) {
            return
        }
        currentProfilers.add(eventId)
        Pury.startProfiling(profilerName(eventId), ROOT_STAGE, 0, 1)
    }

    fun stopProfiling(eventId: String?) {
        if (eventId == null) return
        if (!currentProfilers.contains(eventId)) {
            return
        }
        Pury.stopProfiling(profilerName(eventId), ROOT_STAGE, 1)
        currentProfilers.remove(eventId)
    }

    private fun profilerName(eventId: String) = "SEND_PROFILER_$eventId"

    fun startStage(eventId: String, stage: Stage) {
        if (!currentProfilers.contains(eventId)) return
        Pury.startProfiling(profilerName(eventId), stage.name, stage.ordinal + 1, 1)
    }

    fun stopStage(eventId: String, stage: Stage) {
        if (!currentProfilers.contains(eventId)) return
        Pury.stopProfiling(profilerName(eventId), stage.name, 1)
    }

}
