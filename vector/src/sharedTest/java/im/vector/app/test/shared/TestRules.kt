/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.shared

import net.lachlanmckee.timberjunit.TimberTestRule

fun createTimberTestRule(): TimberTestRule {
    return TimberTestRule.builder()
            .showThread(false)
            .showTimestamp(false)
            .onlyLogWhenTestFails(false)
            .build()
}
