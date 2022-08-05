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

import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.await
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.util.isValidUrl
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

@SessionScope
internal class RealmContentScannerStore @Inject constructor(
        @ContentScannerDatabase
        private val realmInstance: RealmInstance,
        private val clock: Clock,
) : ContentScannerStore {

    override fun getScannerUrl(): String? {
        return realmInstance.getBlockingRealm()
                .queryContentScannerInfoEntity()
                .first()
                .find()
                ?.serverUrl
    }

    override suspend fun setScannerUrl(url: String?) = upsertContentScannerInfoEntity {
        it.serverUrl = url
    }

    override suspend fun enableScanner(enabled: Boolean) = upsertContentScannerInfoEntity {
        it.enabled = enabled
    }

    private suspend fun upsertContentScannerInfoEntity(operation: (ContentScannerInfoEntity) -> Unit) {
        realmInstance.write {
            val contentScannerInfoEntity = queryContentScannerInfoEntity().first().find()
            if (contentScannerInfoEntity != null) {
                operation(contentScannerInfoEntity)
            } else {
                val newContentScannerInfoEntity = ContentScannerInfoEntity().apply {
                    operation(this)
                }
                copyToRealm(newContentScannerInfoEntity)
            }
        }
    }

    override fun isScanEnabled(): Boolean {
        val contentScannerInfoEntity = realmInstance.getBlockingRealm()
                .queryContentScannerInfoEntity()
                .first()
                .find() ?: return false

        return contentScannerInfoEntity.enabled.orFalse() && contentScannerInfoEntity.serverUrl?.isValidUrl().orFalse()
    }

    override suspend fun updateStateForContent(mxcUrl: String, state: ScanState, scannerUrl: String?) = upsertContentScanResultEntity(mxcUrl, scannerUrl) {
        it.scanResult = state
    }

    override suspend fun updateScanResultForContent(mxcUrl: String, scannerUrl: String?, state: ScanState, humanReadable: String) = upsertContentScanResultEntity(
            mxcUrl,
            scannerUrl
    ) {
        it.scanResult = state
        it.scanDateTimestamp = clock.epochMillis()
        it.humanReadableMessage = humanReadable
    }

    private suspend fun upsertContentScanResultEntity(mxcUrl: String, scannerUrl: String?, operation: (ContentScanResultEntity) -> Unit) {
        realmInstance.write {
            val contentScanResultEntity = queryContentScanResultEntity(mxcUrl, scannerUrl).first().find()
            if (contentScanResultEntity != null) {
                operation(contentScanResultEntity)
            } else {
                val newContentScanResultEntity = ContentScanResultEntity().apply {
                    this.mediaUrl = mxcUrl
                    this.scannerUrl = scannerUrl
                    operation(this)
                }
                copyToRealm(newContentScanResultEntity)
            }
        }
    }

    override suspend fun isScanResultKnownOrInProgress(mxcUrl: String, scannerUrl: String?): Boolean {
        val info = realmInstance.getRealm()
                .queryContentScanResultEntity(mxcUrl, scannerUrl)
                .first()
                .await()
                ?.scanResult

        return when (info) {
            ScanState.IN_PROGRESS,
            ScanState.TRUSTED,
            ScanState.INFECTED -> true
            else -> false
        }
    }

    override suspend fun getScanResult(mxcUrl: String): ScanStatusInfo? {
        val scannerUrl = getScannerUrl()
        return realmInstance.getRealm()
                .queryContentScanResultEntity(mxcUrl, scannerUrl)
                .first()
                .await()
                ?.toModel()
    }

    override fun getLiveScanResult(mxcUrl: String): Flow<Optional<ScanStatusInfo>> {
        return realmInstance.getRealmFlow()
                .flatMapConcat { realm ->
                    val scannerUrl = getScannerUrl()
                    realm
                            .queryContentScanResultEntity(mxcUrl, scannerUrl)
                            .first()
                            .asFlow()
                }.map {
                    val scanStatusInfo = it.obj?.toModel()
                    Optional.from(scanStatusInfo)
                }
    }

    private fun TypedRealm.queryContentScanResultEntity(mxcUrl: String, scannerUrl: String?): RealmQuery<ContentScanResultEntity> {
        return query(ContentScanResultEntity::class, "mediaUrl == $0", mxcUrl)
                .andIf(scannerUrl != null) {
                    query("scannerUrl == $0", scannerUrl)
                }
    }

    private fun TypedRealm.queryContentScannerInfoEntity(): RealmQuery<ContentScannerInfoEntity> {
        return query(ContentScannerInfoEntity::class)
    }
}
