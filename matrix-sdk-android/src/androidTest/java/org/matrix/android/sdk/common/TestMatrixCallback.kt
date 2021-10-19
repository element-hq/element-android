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

package org.matrix.android.sdk.common

import androidx.annotation.CallSuper
import org.junit.Assert.fail
import org.matrix.android.sdk.api.MatrixCallback
import timber.log.Timber
import java.util.concurrent.CountDownLatch

/**
 * Simple implementation of MatrixCallback, which count down the CountDownLatch on each API callback
 * @param onlySuccessful true to fail if an error occurs. This is the default behavior
 * @param <T>
 */
open class TestMatrixCallback<T>(private val countDownLatch: CountDownLatch,
                                 private val onlySuccessful: Boolean = true) : MatrixCallback<T> {

    @CallSuper
    override fun onSuccess(data: T) {
        countDownLatch.countDown()
    }

    @CallSuper
    override fun onFailure(failure: Throwable) {
        Timber.e(failure, "TestApiCallback")

        if (onlySuccessful) {
            fail("onFailure " + failure.localizedMessage)
        }

        countDownLatch.countDown()
    }
}
