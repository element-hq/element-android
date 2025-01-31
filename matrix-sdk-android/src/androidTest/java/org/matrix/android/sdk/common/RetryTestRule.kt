/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.common

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Retry test rule used to retry test that failed.
 * Retry failed test 3 times
 */
class RetryTestRule(val retryCount: Int = 3) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return statement(base)
    }

    private fun statement(base: Statement): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                var caughtThrowable: Throwable? = null

                // implement retry logic here
                for (i in 0 until retryCount) {
                    try {
                        base.evaluate()
                        if (i > 0) {
                            println("Retried test $i times")
                        }
                        return
                    } catch (t: Throwable) {
                        caughtThrowable = t
                    }
                }
                throw caughtThrowable!!
            }
        }
    }
}
