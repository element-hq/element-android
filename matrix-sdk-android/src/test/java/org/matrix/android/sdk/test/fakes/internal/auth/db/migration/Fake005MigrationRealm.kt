/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes.internal.auth.db.migration

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import io.realm.DynamicRealm
import io.realm.RealmObjectSchema
import io.realm.RealmSchema
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntityFields

class Fake005MigrationRealm {

    val instance: DynamicRealm = mockk()

    private val schema: RealmSchema = mockk()
    private val objectSchema: RealmObjectSchema = mockk()

    init {
        every { instance.schema } returns schema
        every { schema.get("SessionParamsEntity") } returns objectSchema
        every { objectSchema.addField(any(), any()) } returns objectSchema
        every { objectSchema.setRequired(any(), any()) } returns objectSchema
        every { objectSchema.transform(any()) } returns objectSchema
    }

    fun verifyLoginTypeAdded() {
        verifyLoginTypeFieldAddedAndTransformed()
    }

    private fun verifyLoginTypeFieldAddedAndTransformed() {
        verifyOrder {
            objectSchema["SessionParamsEntity"]
            objectSchema.addField(SessionParamsEntityFields.LOGIN_TYPE, String::class.java)
            objectSchema.setRequired(SessionParamsEntityFields.LOGIN_TYPE, true)
            objectSchema.transform(any())
        }
    }
}
