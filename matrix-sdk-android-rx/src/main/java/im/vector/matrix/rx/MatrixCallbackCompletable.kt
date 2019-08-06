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

package im.vector.matrix.rx

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import io.reactivex.CompletableEmitter
import io.reactivex.SingleEmitter

internal class MatrixCallbackCompletable<T>(private val completableEmitter: CompletableEmitter) : MatrixCallback<T> {

    override fun onSuccess(data: T) {
        completableEmitter.onComplete()
    }

    override fun onFailure(failure: Throwable) {
        completableEmitter.tryOnError(failure)
    }
}

fun Cancelable.toCompletable(completableEmitter: CompletableEmitter) {
    completableEmitter.setCancellable {
        this.cancel()
    }
}