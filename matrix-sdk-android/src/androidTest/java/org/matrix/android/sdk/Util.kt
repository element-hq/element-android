/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
