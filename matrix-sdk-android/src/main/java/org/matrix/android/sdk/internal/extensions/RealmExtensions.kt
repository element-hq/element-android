/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.extensions

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmObjectSchema
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntityFields
import org.matrix.android.sdk.internal.util.fatalError

internal fun RealmObject.assertIsManaged() {
    check(isManaged) { "${javaClass.simpleName} entity should be managed to use this function" }
}

/**
 * Clear a RealmList by deleting all its items calling the provided lambda.
 * The lambda is supposed to delete the item, which means that after this operation, the list will be empty.
 */
internal fun <T> RealmList<T>.clearWith(delete: (T) -> Unit) {
    map { item ->
        // Create a lambda for all items of the list
        { delete(item) }
    }.forEach { lambda ->
        // Then invoke all the lambda
        lambda.invoke()
    }

    if (isNotEmpty()) {
        fatalError("`clearWith` MUST delete all elements of the RealmList")
    }
}

/**
 * Schedule a refresh of the HomeServers capabilities.
 */
internal fun RealmObjectSchema?.forceRefreshOfHomeServerCapabilities(): RealmObjectSchema? {
    return this?.transform { obj ->
        obj.setLong(HomeServerCapabilitiesEntityFields.LAST_UPDATED_TIMESTAMP, 0)
    }
}
