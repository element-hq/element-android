/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.sdk.internal.session.contentscanning.data.ContentScanningStore
import org.matrix.android.sdk.api.session.contentscanning.ScanState
import org.matrix.android.sdk.api.session.contentscanning.ScanStatusInfo
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.isValidUrl
import javax.inject.Inject

@SessionScope
internal class RealmContentScannerStore @Inject constructor(
        @ContentScannerDatabase
        private val realmConfiguration: RealmConfiguration
) : ContentScanningStore {

    private val monarchy = Monarchy.Builder()
            .setRealmConfiguration(realmConfiguration)
            .build()

    override fun getScannerUrl(): String? {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<ContentScannerInfoEntity>()
                }, {
            it.serverUrl
        }
        ).firstOrNull()
    }

    override fun setScannerUrl(url: String?) {
        monarchy.runTransactionSync { realm ->
            val info = realm.where<ContentScannerInfoEntity>().findFirst()
                    ?: realm.createObject()
            info.serverUrl = url
        }
    }

    override fun enableScanning(enabled: Boolean) {
        monarchy.runTransactionSync { realm ->
            val info = realm.where<ContentScannerInfoEntity>().findFirst()
                    ?: realm.createObject()
            info.enabled = enabled
        }
    }

    override fun isScanEnabled(): Boolean {
        return monarchy.fetchAllMappedSync(
                { realm ->
                    realm.where<ContentScannerInfoEntity>()
                }, {
            it.enabled.orFalse() && it.serverUrl?.isValidUrl().orFalse()
        }
        ).firstOrNull().orFalse()
    }

    override fun updateStateForContent(mxcUrl: String, state: ScanState, scannerUrl: String?) {
        monarchy.runTransactionSync {
            ContentScanResultEntity.getOrCreate(it, mxcUrl, scannerUrl).scanResult = state
        }
    }

    override fun updateScanResultForContent(mxcUrl: String, scannerUrl: String?, state: ScanState, humanReadable: String) {
        monarchy.runTransactionSync {
            ContentScanResultEntity.getOrCreate(it, mxcUrl, scannerUrl).apply {
                scanResult = state
                scanDateTimestamp = System.currentTimeMillis()
                humanReadableMessage = humanReadable
            }
        }
    }

    override fun isScanResultKnownOrInProgress(mxcUrl: String, scannerUrl: String?): Boolean {
        var isKnown = false
        monarchy.runTransactionSync {
            val info = ContentScanResultEntity.get(it, mxcUrl, scannerUrl)?.scanResult
            isKnown = when (info) {
                ScanState.IN_PROGRESS,
                ScanState.TRUSTED,
                ScanState.INFECTED -> true
                else               -> false
            }
        }
        return isKnown
    }

    override fun getScanResult(mxcUrl: String): ScanStatusInfo? {
        return monarchy.fetchAllMappedSync({ realm ->
            realm.where<ContentScanResultEntity>()
                    .equalTo(ContentScanResultEntityFields.MEDIA_URL, mxcUrl)
                    .apply {
                        getScannerUrl()?.let {
                            equalTo(ContentScanResultEntityFields.SCANNER_URL, it)
                        }
                    }
        }, {
            it.toModel()
        }).firstOrNull()
    }

    override fun getLiveScanResult(mxcUrl: String): LiveData<Optional<ScanStatusInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<ContentScanResultEntity>()
                            .equalTo(ContentScanResultEntityFields.MEDIA_URL, mxcUrl)
                            .equalTo(ContentScanResultEntityFields.SCANNER_URL, getScannerUrl())
                },
                { entity ->
                    entity.toModel()
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }
}
