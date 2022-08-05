/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database

import io.realm.kotlin.Deleteable
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.dynamic.DynamicMutableRealmObject
import io.realm.kotlin.dynamic.DynamicRealmObject
import io.realm.kotlin.migration.AutomaticSchemaMigration
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.flow.first
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.util.fatalError

internal suspend fun <T : RealmObject> RealmSingleQuery<T>.await(): T? {
    return asFlow().first().obj
}

internal suspend fun <T : RealmObject> RealmQuery<T>.await(): List<T> {
    return asFlow().first().list
}

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

internal fun <T : RealmObject> RealmQuery<T>.queryIn(
        field: String,
        values: List<String>
): RealmQuery<T> {
    if (values.isEmpty()) return this
    val filter = buildString {
        val iterator = values.iterator()
        while (iterator.hasNext()) {
            append("$field == \"${iterator.next()}\"")
            if (iterator.hasNext()) {
                append(" or ")
            }
        }
    }
    return query(filter)
}

internal fun <T : RealmObject> RealmQuery<T>.andIf(
        condition: Boolean,
        builder: RealmQuery<T>.() -> RealmQuery<T>
): RealmQuery<T> {
    return if (condition) {
        builder()
    } else {
        this
    }
}

internal fun MutableRealm.deleteAll() {
    configuration.schema.forEach { kClass ->
        delete(query(kClass).find())
    }
}

internal fun MutableRealm.deleteNullable(deleteable: Deleteable?) {
    if (deleteable == null) return
    delete(deleteable)
}

internal fun AutomaticSchemaMigration.MigrationContext.safeEnumerate(
        className: String,
        block: (oldObject: DynamicRealmObject, newObject: DynamicMutableRealmObject?) -> Unit
) {
    val find = oldRealm.query(className).find()
    find.forEach {
        // TODO OPTIMIZE Using find latest on every object is inefficient
        val latest = tryOrNull("Can't find item in the new db") {
            newRealm.findLatest(it)
        }
        block(it, latest)
    }
}

