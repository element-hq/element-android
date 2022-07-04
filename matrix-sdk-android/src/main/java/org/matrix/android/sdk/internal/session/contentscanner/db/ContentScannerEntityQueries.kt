/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner.db

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

internal fun ContentScanResultEntity.Companion.get(realm: Realm, attachmentUrl: String, contentScannerUrl: String?): ContentScanResultEntity? {
    return realm.where<ContentScanResultEntity>()
            .equalTo(ContentScanResultEntityFields.MEDIA_URL, attachmentUrl)
            .apply {
                contentScannerUrl?.let {
                    equalTo(ContentScanResultEntityFields.SCANNER_URL, it)
                }
            }
            .findFirst()
}

internal fun ContentScanResultEntity.Companion.getOrCreate(
        realm: Realm,
        attachmentUrl: String,
        contentScannerUrl: String?,
        currentTimeMillis: Long
): ContentScanResultEntity {
    return ContentScanResultEntity.get(realm, attachmentUrl, contentScannerUrl)
            ?: realm.createObject<ContentScanResultEntity>().also {
                it.mediaUrl = attachmentUrl
                it.scanDateTimestamp = currentTimeMillis
                it.scannerUrl = contentScannerUrl
            }
}
