/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk

import junit.framework.TestCase.fail

/**
 * Will fail the test if invoking [block] is not throwing a Throwable.
 *
 * @param message the failure message, if the block does not throw any Throwable
 * @param failureBlock a Lambda to be able to do extra check on the thrown Throwable
 * @param block the block to test
 */
internal suspend fun mustFail(
        message: String = "must fail",
        failureBlock: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit,
) {
    val isSuccess = try {
        block.invoke()
        true
    } catch (throwable: Throwable) {
        failureBlock?.invoke(throwable)
        false
    }

    if (isSuccess) {
        fail(message)
    }
}
