/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RealmSessionStoreMigrationTest {

    @Test
    fun `when creating multiple migration instances then they are equal`() {
        RealmSessionStoreMigration(normalizer = mockk()) shouldBeEqualTo RealmSessionStoreMigration(normalizer = mockk())
    }
}
