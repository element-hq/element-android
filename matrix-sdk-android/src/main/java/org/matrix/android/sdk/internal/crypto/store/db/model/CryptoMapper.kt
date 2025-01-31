/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.store.db.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.di.SerializeNulls
import timber.log.Timber

internal object CryptoMapper {

    private val moshi = Moshi.Builder().add(SerializeNulls.JSON_ADAPTER_FACTORY).build()
    private val listMigrationAdapter = moshi.adapter<List<String>>(
            Types.newParameterizedType(
                    List::class.java,
                    String::class.java,
                    Any::class.java
            )
    )
    private val mapMigrationAdapter = moshi.adapter<JsonDict>(
            Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
            )
    )
    private val mapOfStringMigrationAdapter = moshi.adapter<Map<String, Map<String, String>>>(
            Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
            )
    )

    internal fun mapToEntity(deviceInfo: CryptoDeviceInfo): DeviceInfoEntity {
        return DeviceInfoEntity(primaryKey = DeviceInfoEntity.createPrimaryKey(deviceInfo.userId, deviceInfo.deviceId))
                .also { updateDeviceInfoEntity(it, deviceInfo) }
    }

    internal fun updateDeviceInfoEntity(entity: DeviceInfoEntity, deviceInfo: CryptoDeviceInfo) {
        entity.userId = deviceInfo.userId
        entity.deviceId = deviceInfo.deviceId
        entity.algorithmListJson = listMigrationAdapter.toJson(deviceInfo.algorithms)
        entity.keysMapJson = mapMigrationAdapter.toJson(deviceInfo.keys)
        entity.signatureMapJson = mapMigrationAdapter.toJson(deviceInfo.signatures)
        entity.isBlocked = deviceInfo.isBlocked
        val deviceInfoTrustLevel = deviceInfo.trustLevel
        if (deviceInfoTrustLevel == null) {
            entity.trustLevelEntity?.deleteFromRealm()
            entity.trustLevelEntity = null
        } else {
            if (entity.trustLevelEntity == null) {
                // Create a new TrustLevelEntity object
                entity.trustLevelEntity = TrustLevelEntity()
            }
            // Update the existing TrustLevelEntity object
            entity.trustLevelEntity?.crossSignedVerified = deviceInfoTrustLevel.crossSigningVerified
            entity.trustLevelEntity?.locallyVerified = deviceInfoTrustLevel.locallyVerified
        }
        // We store the device name if present now
        entity.unsignedMapJson = deviceInfo.unsigned?.deviceDisplayName
    }

    internal fun mapToModel(deviceInfoEntity: DeviceInfoEntity): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                userId = deviceInfoEntity.userId ?: "",
                deviceId = deviceInfoEntity.deviceId ?: "",
                isBlocked = deviceInfoEntity.isBlocked ?: false,
                trustLevel = deviceInfoEntity.trustLevelEntity?.let {
                    DeviceTrustLevel(it.crossSignedVerified ?: false, it.locallyVerified)
                },
                unsigned = deviceInfoEntity.unsignedMapJson?.let { UnsignedDeviceInfo(deviceDisplayName = it) },
                signatures = deviceInfoEntity.signatureMapJson?.let {
                    try {
                        mapOfStringMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                keys = deviceInfoEntity.keysMapJson?.let {
                    try {
                        moshi.adapter<Map<String, String>>(
                                Types.newParameterizedType(
                                        Map::class.java,
                                        String::class.java,
                                        Any::class.java
                                )
                        ).fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                algorithms = deviceInfoEntity.algorithmListJson?.let {
                    try {
                        listMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                firstTimeSeenLocalTs = deviceInfoEntity.firstTimeSeenLocalTs
        )
    }
}
