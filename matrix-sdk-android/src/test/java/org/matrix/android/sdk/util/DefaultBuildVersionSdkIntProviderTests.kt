/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.util

import android.os.Build
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.util.DefaultBuildVersionSdkIntProvider

class DefaultBuildVersionSdkIntProviderTests {

    @Test
    fun getReturnsCurrentVersionFromBuild_Version_SDK_INT() {
        val provider = DefaultBuildVersionSdkIntProvider()
        provider.get() shouldBeEqualTo Build.VERSION.SDK_INT
    }
}
