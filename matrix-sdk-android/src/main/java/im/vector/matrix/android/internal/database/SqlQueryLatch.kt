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

package im.vector.matrix.android.internal.database

import com.squareup.sqldelight.Query
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal suspend fun Query<Boolean>.awaitResult(timeoutMillis: Long) = withTimeout(timeoutMillis) {
    withContext(Dispatchers.IO) {
        val exists = executeAsOne()
        if (exists) {
            return@withContext
        }
        val latch = CompletableDeferred<Unit>()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                if (executeAsOne()) {
                    latch.complete(Unit)
                }
            }
        }
        addListener(listener)
        try {
            latch.await()
        } finally {
            removeListener(listener)
        }
    }
}
