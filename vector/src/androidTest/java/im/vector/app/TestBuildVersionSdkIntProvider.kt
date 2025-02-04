/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider

class TestBuildVersionSdkIntProvider(var value: Int = 0) : BuildVersionSdkIntProvider {
    override fun get() = value
}
