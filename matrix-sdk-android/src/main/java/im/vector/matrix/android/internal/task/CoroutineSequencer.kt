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
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        launchCoroutine()
    }

    private fun launchCoroutine() {
        // This will ensure suspend methods run sequentially (in a different thread and context).
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
        coroutineScope.cancel()
        messageChannel.close()
        singleDispatcher.close() // To release the thread in the pool.
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

internal open class SimpleMutexCoroutineSequencer<T> : CoroutineSequencer<T> {
    private val mutex = Mutex()

    override suspend fun post(block: suspend () -> T): T {
        return mutex.withLock { block() }
    }

    override fun close() {
        // Not strictly necessary, since there's no resource to be released.
    }
}

internal open class MutexCoroutineSequencer<T> : CoroutineSequencer<T> {
    private val mutex = Mutex()
    // Will complete once this sequencer is closed.
    private val cancelToken = CompletableDeferred<Unit>()

    override suspend fun post(block: suspend () -> T): T {
        check(!cancelToken.isCompleted) { "Cannot post in cancelled sequencer." }

        return select {
            cancelToken.onAwait {
                throw CancellationException("Sequencer was cancelled.")
            }
            mutex.onLock {
                try {
                    block()
                } finally {
                    it.unlock()
                }
            }
        }
    }

    override fun close() {
        cancelToken.complete(Unit)
    }
}

// This is actually more efficient than the Mutex one.
internal open class SimpleSemaphoreCoroutineSequencer<T> : CoroutineSequencer<T> {
    private val semaphore = Semaphore(1) // Permits 1 suspend function at a time.

    override suspend fun post(block: suspend () -> T): T {
        return semaphore.withPermit { block() }
    }

    override fun close() {
        // Not strictly necessary, since there's no resource to be released.
    }
}

internal open class SemaphoreCoroutineSequencer<T> : CoroutineSequencer<T> {
    private val semaphore = Semaphore(1) // Permits 1 suspend function at a time.
    private val isClosed = AtomicBoolean(false) // Made this atomic for correctness.

    override suspend fun post(block: suspend () -> T): T {
        check(!isClosed.get()) { "Cannot post in cancelled sequencer." }

        return semaphore.withPermit {
            if (isClosed.get()) throw CancellationException("Sequencer was cancelled.")
            block()
        }
    }

    override fun close() {
        isClosed.set(false)
    }
}
