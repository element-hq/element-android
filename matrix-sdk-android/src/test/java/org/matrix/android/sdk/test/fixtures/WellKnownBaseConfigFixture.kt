/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fixtures

import org.matrix.android.sdk.api.auth.data.WellKnownBaseConfig

object WellKnownBaseConfigFixture {
    fun aWellKnownBaseConfig(
            baseUrl: String? = null,
    ) = WellKnownBaseConfig(
            baseUrl,
    )
}
