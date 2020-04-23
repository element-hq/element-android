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
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

internal interface LiveEntityObserver {
    fun start()
    fun dispose()
    fun cancelProcess()
    fun isStarted(): Boolean
}

internal abstract class SqlLiveEntityObserver<T : Any>(protected val sessionDatabase: SessionDatabase)
    : LiveEntityObserver, Query.Listener {

    protected val observerScope = CoroutineScope(SupervisorJob())
    protected abstract val query: Query<T>
    private val isStarted = AtomicBoolean(false)

    protected abstract suspend fun handleChanges(results: List<T>)

    override fun queryResultsChanged() {
        observerScope.launch(Dispatchers.Default) {
            val results = query.executeAsList()
            if(results.isNotEmpty()) {
                handleChanges(results)
            }
        }
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            query.addListener(this)
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            query.removeListener(this)
            observerScope.coroutineContext.cancelChildren()
        }
    }

    override fun cancelProcess() {
        observerScope.coroutineContext.cancelChildren()
    }

    override fun isStarted(): Boolean {
        return isStarted.get()
    }

}
