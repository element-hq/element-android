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

package org.matrix.android.sdk.api.util.profiling

import com.nikitakozlov.pury.Pury

private const val ROOT_STAGE = "ROOT_STAGE"

/**
 * Base class for profilers, it's implementing Pury start and stop profiling.
 * Order of STAGE in enum matters.
 */
abstract class BaseProfiler<STAGE : Enum<STAGE>> {

    private val currentProfilers = ArrayList<String>()

    abstract val name: String

    /**
     * Use to start the profiling process. It's necessary to call this method before any other method.
     * You should always call stop profiling with the same key param when you want to stop profiling.
     *
     * @param key unique identifier of the profiling.
     */
    fun startProfiling(key: String) {
        if (currentProfilers.contains(key)) {
            return
        }
        currentProfilers.add(key)
        Pury.startProfiling(profilerName(key), ROOT_STAGE, 0, 1)
    }

    /**
     * Use to stop the profiling process. This will dispatch information to the configured pury plugins.
     *
     * @param key unique identifier of the profiling.
     */
    fun stopProfiling(key: String) {
        if (!currentProfilers.contains(key)) {
            return
        }
        Pury.stopProfiling(profilerName(key), ROOT_STAGE, 1)
        currentProfilers.remove(key)
    }

    /**
     * Use to profile a block of code. Internally it will call start and stop stage.
     *
     * @param key unique identifier of the profiling.
     * @param stage the current stage to mark
     */
    fun profileBlock(key: String, stage: STAGE, block: (() -> Unit)? = null) {
        startStage(key, stage)
        block?.invoke()
        stopStage(key, stage)
    }

    /**
     * Use to add a new stage to profile inside the root profiling.
     * You should have called startProfiling with the same key before.
     * You should also call stopStage with same key and same stage to mark the end of this stage.
     *
     * @param key unique identifier of the profiling.
     * @param stage the current stage to profile
     */
    fun startStage(key: String, stage: STAGE) {
        if (!currentProfilers.contains(key)) return
        Pury.startProfiling(profilerName(key), stage.name, stage.ordinal + 1, 1)
    }

    /**
     * Use to finish the profiling of a stage.
     * You should have called startStage with the same key and same stage before.
     *
     * @param key unique identifier of the profiling.
     * @param stage the current stage to profile
     */
    fun stopStage(key: String, stage: STAGE) {
        if (!currentProfilers.contains(key)) return
        Pury.stopProfiling(profilerName(key), stage.name, 1)
    }

    private fun profilerName(key: String) = "${name}_$key"
}
