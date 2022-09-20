/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun <T, R> T.onMain(block: T.() -> R): R {
    return withContext(Dispatchers.Main) {
        block(this@onMain)
    }
}

suspend fun <T> LiveData<T>.first(timeout: Long = TestConstants.timeOutMillis, predicate: (T) -> Boolean): T {
    return wrapWithTimeout(timeout) {
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                val observer = object : Observer<T> {
                    override fun onChanged(data: T) {
                        if (predicate(data)) {
                            removeObserver(this)
                            continuation.resume(data)
                        }
                    }
                }
                observeForever(observer)
            }
        }
    }
}

suspend fun <T> waitFor(continueWhen: suspend () -> T, action: suspend () -> Unit) {
    coroutineScope {
        val deferred = async { continueWhen() }
        action()
        deferred.await()
    }
}

suspend fun <T> wrapWithTimeout(timeout: Long = TestConstants.timeOutMillis, block: suspend () -> T): T {
    val deferred = coroutineScope {
        async { block() }
    }
    return withTimeout(timeout) { deferred.await() }
}
