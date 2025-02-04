/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test

import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider

class TestBuildVersionSdkIntProvider : BuildVersionSdkIntProvider {
    var value: Int = 0

    override fun get() = value
}
