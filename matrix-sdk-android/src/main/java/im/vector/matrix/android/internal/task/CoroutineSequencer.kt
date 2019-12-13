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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Executors

/**
 * This class intends to be used for ensure suspendable methods are played sequentially all the way long.
 */
internal interface CoroutineSequencer<T> {
    /**
     * @param block the suspendable block to execute
     * @return the result of the block
     */
    suspend fun post(block: suspend () -> T): T

    /**
     * Cancel all and close, so you won't be able to post anything else after
     */
    fun close()
}

internal open class ChannelCoroutineSequencer<T> : CoroutineSequencer<T> {

    private data class Message<T>(
            val block: suspend () -> T,
            val deferred: CompletableDeferred<T>
    )

    private var messageChannel: Channel<Message<T>> = Channel()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    // This will ensure
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
        coroutineScope.coroutineContext.cancelChildren()
        messageChannel.close()
    }

    override suspend fun post(block: suspend () -> T): T {
        val deferred = CompletableDeferred<T>()
        val message = Message(block, deferred)
        messageChannel.send(message)
        return try {
            deferred.await()
        } catch (cancellation: CancellationException) {
            // In case of cancellation, we stop the current coroutine context
            // and relaunch one to consume next messages
            coroutineScope.coroutineContext.cancelChildren()
            launchCoroutine()
            throw cancellation
        }
    }
}
