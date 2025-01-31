/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
open class TestMatrixCallback<T>(
        private val countDownLatch: CountDownLatch,
        private val onlySuccessful: Boolean = true
) : MatrixCallback<T> {

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
