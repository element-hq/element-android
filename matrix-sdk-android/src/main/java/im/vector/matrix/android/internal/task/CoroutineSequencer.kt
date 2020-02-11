/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.task

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * This class intends to be used for ensure suspendable methods are played sequentially all the way long.
 */
internal interface CoroutineSequencer {
    /**
     * @param block the suspendable block to execute
     * @return the result of the block
     */
    suspend fun <T> post(block: suspend () -> T): T
}

internal open class SemaphoreCoroutineSequencer : CoroutineSequencer {

    private val semaphore = Semaphore(1) // Permits 1 suspend function at a time.

    override suspend fun <T> post(block: suspend () -> T): T {
        return semaphore.withPermit {
            block()
        }
    }
}
