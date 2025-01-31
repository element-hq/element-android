/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.matrix.android.sdk.common.RetryTestRule
import org.matrix.android.sdk.test.shared.createTimberTestRule

interface InstrumentedTest {

    @Rule
    fun retryTestRule() = RetryTestRule(3)

    @Rule
    fun timberTestRule() = createTimberTestRule()

    fun context(): Context {
        return ApplicationProvider.getApplicationContext()
    }
}
