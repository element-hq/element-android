/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util.database

import io.realm.DynamicRealm
import io.realm.RealmObjectSchema
import timber.log.Timber
import kotlin.system.measureTimeMillis

internal abstract class RealmMigrator(
        private val realm: DynamicRealm,
        private val targetSchemaVersion: Int
) {
    fun perform() {
        Timber.d("Migrate ${realm.configuration.realmFileName} to $targetSchemaVersion")
        val duration = measureTimeMillis {
            doMigrate(realm)
        }
        Timber.d("Migrate ${realm.configuration.realmFileName} to $targetSchemaVersion took $duration ms.")
    }

    abstract fun doMigrate(realm: DynamicRealm)

    protected fun RealmObjectSchema.addFieldIfNotExists(fieldName: String, fieldType: Class<*>): RealmObjectSchema {
        if (!hasField(fieldName)) {
            addField(fieldName, fieldType)
        }
        return this
    }

    protected fun RealmObjectSchema.removeFieldIfExists(fieldName: String): RealmObjectSchema {
        if (hasField(fieldName)) {
            removeField(fieldName)
        }
        return this
    }

    protected fun RealmObjectSchema.setRequiredIfNotAlready(fieldName: String, isRequired: Boolean): RealmObjectSchema {
        if (isRequired != isRequired(fieldName)) {
            setRequired(fieldName, isRequired)
        }
        return this
    }
}
