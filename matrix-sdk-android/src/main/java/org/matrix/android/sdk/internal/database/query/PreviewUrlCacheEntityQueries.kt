/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntity

/**
 * Get the current PreviewUrlCacheEntity, return null if it does not exist.
 */
internal fun PreviewUrlCacheEntity.Companion.get(realm: TypedRealm, url: String): PreviewUrlCacheEntity? {
    return realm.query(PreviewUrlCacheEntity::class)
            .query("url == $0", url)
            .first()
            .find()
}

/**
 * Get the current PreviewUrlCacheEntity, create one if it does not exist.
 */
internal fun PreviewUrlCacheEntity.Companion.getOrCreate(realm: MutableRealm, url: String): PreviewUrlCacheEntity {
    return get(realm, url) ?: create(realm, url)
}

internal fun PreviewUrlCacheEntity.Companion.create(realm: MutableRealm, url: String): PreviewUrlCacheEntity {
    val previewUrlCacheEntity = PreviewUrlCacheEntity().apply {
        this.url = url
    }
    return realm.copyToRealm(previewUrlCacheEntity)
}
