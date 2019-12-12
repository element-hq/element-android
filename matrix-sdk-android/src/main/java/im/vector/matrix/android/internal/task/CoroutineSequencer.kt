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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.intrinsics.startCoroutineCancellable
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

internal interface CoroutineSequencer<T> {
    suspend fun post(block: suspend () -> T): T
    fun cancel()
    fun close()
}

internal class ChannelCoroutineSequencer<T> : CoroutineSequencer<T> {

    private data class Message<T>(
            val block: suspend () -> T,
            val deferred: CompletableDeferred<T>
    )

    private val messageChannel: Channel<Message<T>> = Channel()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        launchCoroutine()
    }

    private fun launchCoroutine() {
        coroutineScope.launch(singleDispatcher) {
            for (message in messageChannel) {
                try {
                    val result = message.block()
                    message.deferred.complete(result)
                } catch (exception: Throwable) {
                    message.deferred.completeExceptionally(exception)
                }
            }
        }
    }

    override fun close() {
        messageChannel.cancel()
        coroutineScope.coroutineContext.cancelChildren()
    }

    override fun cancel() {
        close()
        launchCoroutine()
    }

    override suspend fun post(block: suspend () -> T): T {
        val deferred = CompletableDeferred<T>()
        val message = Message(block, deferred)
        messageChannel.send(message)
        return try {
            deferred.await()
        } catch (cancellation: CancellationException) {
            coroutineScope.coroutineContext.cancelChildren()
            launchCoroutine()
            throw cancellation
        }
    }

}

