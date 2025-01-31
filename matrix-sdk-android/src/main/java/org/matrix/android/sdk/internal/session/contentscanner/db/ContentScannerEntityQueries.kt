/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
