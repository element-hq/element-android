/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db.migration

import org.junit.Test
import org.matrix.android.sdk.test.fakes.internal.auth.db.migration.Fake005MigrationRealm

class MigrateAuthTo005Test {

    private val fakeRealm = Fake005MigrationRealm()
    private val migrator = MigrateAuthTo005(fakeRealm.instance)

    @Test
    fun `when doMigrate, then LoginType field added`() {
        migrator.doMigrate(fakeRealm.instance)

        fakeRealm.verifyLoginTypeAdded()
    }
}
