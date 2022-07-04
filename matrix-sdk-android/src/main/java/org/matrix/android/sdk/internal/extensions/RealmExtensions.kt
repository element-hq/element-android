/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
