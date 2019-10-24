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

package im.vector.riotx.core.utils

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * An interface to handle InMemory Rx Store from which you can post or observe values.
 */
interface RxStore<T> {
    fun observe(): Observable<T>
    fun post(value: T)
}

/**
 * This store emits the most recent value it has observed and all subsequent observed values to each subscriber.
 */
open class BehaviorStore<T>(private val defaultValue: T? = null) : RxStore<T> {

    private val storeRelay = createRelay()

    override fun observe(): Observable<T> {
        return storeRelay.hide().observeOn(Schedulers.computation())
    }

    override fun post(value: T) {
        storeRelay.accept(value)
    }

    private fun createRelay(): BehaviorRelay<T> {
        return if (defaultValue == null) {
            BehaviorRelay.create<T>()
        } else {
            BehaviorRelay.createDefault(defaultValue)
        }
    }
}

/**
 * This store only emits all subsequent observed values to each subscriber.
 */
open class PublishStore<T> : RxStore<T> {

    private val storeRelay = PublishRelay.create<T>()

    override fun observe(): Observable<T> {
        return storeRelay.hide()
    }

    override fun post(value: T) {
        storeRelay.accept(value)
    }
}
