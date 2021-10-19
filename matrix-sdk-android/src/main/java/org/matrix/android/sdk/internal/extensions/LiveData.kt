/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

inline fun <T> LiveData<T>.observeK(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) {
    this.observe(owner, Observer { observer(it) })
}

inline fun <T> LiveData<T>.observeNotNull(owner: LifecycleOwner, crossinline observer: (T) -> Unit) {
    this.observe(owner, Observer { it?.run(observer) })
}

fun <T1, T2, R> combineLatest(source1: LiveData<T1>, source2: LiveData<T2>, mapper: (T1, T2) -> R): LiveData<R> {
    val combined = MediatorLiveData<R>()
    var source1Result: T1? = null
    var source2Result: T2? = null

    fun notify() {
        if (source1Result != null && source2Result != null) {
            combined.value = mapper(source1Result!!, source2Result!!)
        }
    }

    combined.addSource(source1) {
        source1Result = it
        notify()
    }
    combined.addSource(source2) {
        source2Result = it
        notify()
    }
    return combined
}
