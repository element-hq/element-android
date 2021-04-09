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

package im.vector.app.core.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.app.core.utils.EventObserver
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.core.utils.LiveEvent

inline fun <T> LiveData<T>.observeK(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) {
    this.observe(owner, { observer(it) })
}

inline fun <T> LiveData<T>.observeNotNull(owner: LifecycleOwner, crossinline observer: (T) -> Unit) {
    this.observe(owner, { it?.run(observer) })
}

inline fun <T> LiveData<LiveEvent<T>>.observeEvent(owner: LifecycleOwner, crossinline observer: (T) -> Unit) {
    this.observe(owner, EventObserver { it.run(observer) })
}

inline fun <T> LiveData<LiveEvent<T>>.observeEventFirstThrottle(owner: LifecycleOwner, minimumInterval: Long, crossinline observer: (T) -> Unit) {
    val firstThrottler = FirstThrottler(minimumInterval)

    this.observe(owner, EventObserver {
        if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
            it.run(observer)
        }
    })
}

fun <T> MutableLiveData<LiveEvent<T>>.postLiveEvent(content: T) {
    this.postValue(LiveEvent(content))
}
