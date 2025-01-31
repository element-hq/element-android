/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.shared

import net.lachlanmckee.timberjunit.TimberTestRule

internal fun createTimberTestRule(): TimberTestRule {
    return TimberTestRule.builder()
            .showThread(false)
            .showTimestamp(false)
            .onlyLogWhenTestFails(false)
            .build()
}
