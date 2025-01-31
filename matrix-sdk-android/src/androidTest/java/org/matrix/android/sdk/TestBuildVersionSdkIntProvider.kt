/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk

import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider

class TestBuildVersionSdkIntProvider : BuildVersionSdkIntProvider {
    var value: Int = 0

    override fun get() = value
}
