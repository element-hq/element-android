/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import android.os.Build
import javax.inject.Inject

class DefaultBuildVersionSdkIntProvider @Inject constructor() :
        BuildVersionSdkIntProvider {
    override fun get() = Build.VERSION.SDK_INT
}
