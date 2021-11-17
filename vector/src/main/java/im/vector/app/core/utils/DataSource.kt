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

package im.vector.app.core.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface DataSource<T> {
    fun stream(): Flow<T>
}

interface MutableDataSource<T> : DataSource<T> {
    fun post(value: T)
}

/**
 * This datasource emits the most recent value it has observed and all subsequent observed values to each subscriber.
 */
open class BehaviorDataSource<T>(private val defaultValue: T? = null) : MutableDataSource<T> {

    private val mutableFlow = MutableSharedFlow<T>(replay = 1)

    val currentValue: T?
        get() = mutableFlow.replayCache.firstOrNull()

    override fun stream(): Flow<T> {
        return mutableFlow
    }

    override fun post(value: T) {
        mutableFlow.tryEmit(value)
    }
}

/**
 * This datasource only emits all subsequent observed values to each subscriber.
 */
open class PublishDataSource<T> : MutableDataSource<T> {

    private val mutableFlow = MutableSharedFlow<T>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun stream(): Flow<T> {
        return mutableFlow
    }

    override fun post(value: T) {
        mutableFlow.tryEmit(value)
    }
}
