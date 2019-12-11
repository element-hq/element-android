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

import im.vector.matrix.android.internal.di.MatrixScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Executors
import javax.inject.Inject


@MatrixScope
internal class MatrixCoroutineSequencers @Inject constructor() {

    private val sequencers = HashMap<String, CoroutineSequencer>()

    suspend fun post(name: String, block: suspend CoroutineScope.() -> Any): Any {
        val sequencer = sequencers.getOrPut(name) {
            ChannelCoroutineSequencer()
        }
        return sequencer.post(block)
    }

    fun cancel(name: String) {
        sequencers.remove(name)?.cancel()
    }

    fun cancelAll() {
        sequencers.values.forEach {
            it.cancel()
        }
        sequencers.clear()
    }

}

internal interface CoroutineSequencer {
    suspend fun post(block: suspend CoroutineScope.() -> Any): Any
    fun cancel()
}

internal class ChannelCoroutineSequencer : CoroutineSequencer {

    private data class Message(
            val block: suspend CoroutineScope.() -> Any,
            val deferred: CompletableDeferred<Any>
    )

    private val messageChannel: Channel<Message> = Channel()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        coroutineScope.launch(singleDispatcher) {
            for (message in messageChannel) {
                try {
                    val result = message.block(this)
                    message.deferred.complete(result)
                } catch (exception: Throwable) {
                    message.deferred.completeExceptionally(exception)
                }
            }
        }
    }

    override fun cancel() {
        messageChannel.cancel()
        coroutineScope.coroutineContext.cancelChildren()
    }

    override suspend fun post(block: suspend CoroutineScope.() -> Any): Any {
        val deferred = CompletableDeferred<Any>()
        val message = Message(block, deferred)
        messageChannel.send(message)
        return deferred.await()
    }

}

