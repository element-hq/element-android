/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.store.db

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import im.vector.matrix.android.api.extensions.tryThis
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.GossipRequestType
import im.vector.matrix.android.internal.crypto.GossipingRequestState
import im.vector.matrix.android.internal.crypto.IncomingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.IncomingSecretShareRequest
import im.vector.matrix.android.internal.crypto.IncomingShareRequestCommon
import im.vector.matrix.android.internal.crypto.OutgoingGossipingRequest
import im.vector.matrix.android.internal.crypto.OutgoingGossipingRequestState
import im.vector.matrix.android.internal.crypto.OutgoingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.OutgoingSecretRequest
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.CryptoCrossSigningKey
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.SerializeNulls
import im.vector.matrix.sqldelight.crypto.CrossSigningInfoEntity
import im.vector.matrix.sqldelight.crypto.DeviceInfoEntity
import im.vector.matrix.sqldelight.crypto.GetByUserId
import im.vector.matrix.sqldelight.crypto.IncomingGossipingRequestEntity
import im.vector.matrix.sqldelight.crypto.OutgoingGossipingRequestEntity
import timber.log.Timber

object SqliteCryptoMapper {

    private val moshi = Moshi.Builder().add(SerializeNulls.JSON_ADAPTER_FACTORY).build()
    private val listMigrationAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(
            List::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapMigrationAdapter = moshi.adapter<JsonDict>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapOfStringMigrationAdapter = moshi.adapter<Map<String, Map<String, String>>>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))
    private val recipientsDataMapper: JsonAdapter<Map<String, List<String>>> =
            MoshiProvider
                    .providesMoshi()
                    .adapter<Map<String, List<String>>>(
                            Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                    )

    internal fun mapToEntity(deviceInfo: CryptoDeviceInfo): DeviceInfoEntity {
        return DeviceInfoEntity.Impl(
                user_id = deviceInfo.userId,
                device_id = deviceInfo.deviceId,
                identity_key = deviceInfo.identityKey(),
                algorithm_list_json = listMigrationAdapter.toJson(deviceInfo.algorithms),
                keys_map_json = mapMigrationAdapter.toJson(deviceInfo.keys),
                signature_map_json = mapMigrationAdapter.toJson(deviceInfo.signatures),
                is_blocked = deviceInfo.isBlocked,
                locally_verified = deviceInfo.trustLevel?.locallyVerified == true,
                cross_signed_verified = deviceInfo.trustLevel?.crossSigningVerified == true,
                unsigned_map_json = mapMigrationAdapter.toJson(deviceInfo.unsigned)
        )
    }

    internal fun mapToModel(deviceInfoEntity: DeviceInfoEntity): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                deviceId = deviceInfoEntity.device_id,
                userId = deviceInfoEntity.user_id,
                algorithms = deviceInfoEntity.algorithm_list_json?.let {
                    try {
                        listMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                keys = deviceInfoEntity.keys_map_json?.let {
                    try {
                        moshi.adapter<Map<String, String>>(Types.newParameterizedType(
                                Map::class.java,
                                String::class.java,
                                Any::class.java
                        )).fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                signatures = deviceInfoEntity.signature_map_json?.let {
                    try {
                        mapOfStringMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                unsigned = deviceInfoEntity.unsigned_map_json?.let {
                    try {
                        mapMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                trustLevel = DeviceTrustLevel(deviceInfoEntity.cross_signed_verified, deviceInfoEntity.locally_verified),
                isBlocked = deviceInfoEntity.is_blocked
        )
    }

    internal fun mapToModel(crossSigningInfoEntity: GetByUserId): CryptoCrossSigningKey {
        val pubKey = crossSigningInfoEntity.public_key_base64 ?: ""
        return CryptoCrossSigningKey(
                userId = crossSigningInfoEntity.user_id,
                signatures = crossSigningInfoEntity.signatures?.let {
                    try {
                        mapOfStringMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                trustLevel = DeviceTrustLevel(
                        crossSigningInfoEntity.cross_signed_verified,
                        crossSigningInfoEntity.locally_verified
                ),
                keys = mapOf("ed25519:$pubKey" to pubKey),
                usages = crossSigningInfoEntity.usages
        )
    }

    internal fun mapToEntity(cryptoCrossSigningKey: CryptoCrossSigningKey): CrossSigningInfoEntity {
        return CrossSigningInfoEntity.Impl(
                user_id = cryptoCrossSigningKey.userId,
                signatures = mapMigrationAdapter.toJson(cryptoCrossSigningKey.signatures),
                public_key_base64 = cryptoCrossSigningKey.unpaddedBase64PublicKey,
                usages = cryptoCrossSigningKey.usages ?: emptyList(),
                locally_verified = false,
                cross_signed_verified = false
        )
    }

    fun toOutgoingGossipingRequest(entity: OutgoingGossipingRequestEntity): OutgoingGossipingRequest {
        return when (entity.type) {
            GossipRequestType.KEY.name    -> {
                OutgoingRoomKeyRequest(
                        requestBody = getRequestedKeyInfo(entity.type, entity.requested_info),
                        recipients = getRecipientsMap(entity) ?: emptyMap(),
                        requestId = entity.request_id,
                        state = tryThis { OutgoingGossipingRequestState.valueOf(entity.requested_info!!) }
                                ?: OutgoingGossipingRequestState.UNSENT
                )
            }
            GossipRequestType.SECRET.name -> {
                OutgoingSecretRequest(
                        secretName = getRequestedSecretName(entity.type, entity.requested_info),
                        recipients = getRecipientsMap(entity) ?: emptyMap(),
                        requestId = entity.request_id,
                        state = tryThis { OutgoingGossipingRequestState.valueOf(entity.requested_info!!) }
                                ?: OutgoingGossipingRequestState.UNSENT
                )
            }
            else -> OutgoingRoomKeyRequest(
                    requestBody = getRequestedKeyInfo(entity.type, entity.requested_info),
                    recipients = getRecipientsMap(entity) ?: emptyMap(),
                    requestId = entity.request_id,
                    state = OutgoingGossipingRequestState.UNSENT
            )
        }
    }

    fun toIncomingGossipingRequest(entity: IncomingGossipingRequestEntity): IncomingShareRequestCommon {
        return when (GossipRequestType.valueOf(entity.type)) {
            GossipRequestType.KEY -> {
                IncomingRoomKeyRequest(
                        requestBody = getRequestedKeyInfo(entity.type, entity.requested_info_str),
                        deviceId = entity.other_device_id,
                        userId = entity.other_user_id,
                        requestId = entity.request_id,
                        state = entity.request_state?.let { GossipingRequestState.valueOf(it) }
                                ?: GossipingRequestState.NONE,
                        localCreationTimestamp = entity.local_creation_timestamp
                )
            }
            GossipRequestType.SECRET -> {
                IncomingSecretShareRequest(
                        secretName = getRequestedSecretName(entity.type, entity.requested_info_str),
                        deviceId = entity.other_device_id,
                        userId = entity.other_user_id,
                        requestId = entity.request_id,
                        localCreationTimestamp = entity.local_creation_timestamp
                )
            }
        }
    }

    private fun getRecipientsMap(entity: OutgoingGossipingRequestEntity): Map<String, List<String>>? {
        return entity.recipients_data?.let { recipientsDataMapper.fromJson(it) }
    }

    fun getRecipientsData(recipients: Map<String, List<String>>): String? {
        return recipientsDataMapper.toJson(recipients)
    }

    fun getRequestedKeyInfo(type: String, requestedInfoStr: String?): RoomKeyRequestBody? {
        return if (type == GossipRequestType.KEY.name) {
            RoomKeyRequestBody.fromJson(requestedInfoStr)
        } else null
    }

    fun getRequestedSecretName(type: String, requestedInfoStr: String?): String? {
        return if (type == GossipRequestType.SECRET.name) {
            requestedInfoStr
        } else null
    }
}
