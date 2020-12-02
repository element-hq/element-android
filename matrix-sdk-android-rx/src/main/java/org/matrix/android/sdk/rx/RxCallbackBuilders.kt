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

package org.matrix.android.sdk.rx

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable
import io.reactivex.Completable
import io.reactivex.Single

fun <T> singleBuilder(builder: (MatrixCallback<T>) -> Cancelable): Single<T> = Single.create { emitter ->
    val callback = object : MatrixCallback<T> {
        override fun onSuccess(data: T) {
            // Add `!!` to fix the warning:
            // "Type mismatch: type parameter with nullable bounds is used T is used where T was expected. This warning will become an error soon"
            emitter.onSuccess(data!!)
        }

        override fun onFailure(failure: Throwable) {
            emitter.tryOnError(failure)
        }
    }
    val cancelable = builder(callback)
    emitter.setCancellable {
        cancelable.cancel()
    }
}

fun <T> completableBuilder(builder: (MatrixCallback<T>) -> Cancelable): Completable = Completable.create { emitter ->
    val callback = object : MatrixCallback<T> {
        override fun onSuccess(data: T) {
            emitter.onComplete()
        }

        override fun onFailure(failure: Throwable) {
            emitter.tryOnError(failure)
        }
    }
    val cancelable = builder(callback)
    emitter.setCancellable {
        cancelable.cancel()
    }
}
