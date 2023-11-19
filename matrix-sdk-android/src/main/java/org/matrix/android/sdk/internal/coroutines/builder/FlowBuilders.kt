/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.coroutines.builder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope

/**
 * Use this with a flow builder like [kotlinx.coroutines.flow.channelFlow] to replace [kotlinx.coroutines.channels.awaitClose].
 * As awaitClose is at the end of the builder block, it can lead to the block being cancelled before it reaches the awaitClose.
 * Example of usage:
 *
 *  return channelFlow {
 *      val onClose = safeInvokeOnClose {
 *          // Do stuff on close
 *      }
 *      val data = getData()
 *      send(data)
 *      onClose.await()
 * }
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> ProducerScope<T>.safeInvokeOnClose(handler: (cause: Throwable?) -> Unit): CompletableDeferred<Unit> {
    val onClose = CompletableDeferred<Unit>()
    invokeOnClose {
        handler(it)
        onClose.complete(Unit)
    }
    return onClose
}
