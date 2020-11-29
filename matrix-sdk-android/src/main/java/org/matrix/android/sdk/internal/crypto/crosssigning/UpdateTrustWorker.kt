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

package org.matrix.android.sdk.internal.crypto.crosssigning

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import timber.log.Timber
import javax.inject.Inject

internal class UpdateTrustWorker(context: Context,
                                 params: WorkerParameters)
    : SessionSafeCoroutineWorker<UpdateTrustWorker.Params>(context, params, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            override val lastFailureMessage: String? = null,
            val updatedUserIds: List<String>
    ) : SessionWorkerParams

    @Inject lateinit var crossSigningService: DefaultCrossSigningService

    // It breaks the crypto store contract, but we need to batch things :/
    @CryptoDatabase @Inject lateinit var realmConfiguration: RealmConfiguration
    @UserId @Inject lateinit var myUserId: String
    @Inject lateinit var crossSigningKeysMapper: CrossSigningKeysMapper
    @SessionDatabase @Inject lateinit var sessionRealmConfiguration: RealmConfiguration

    //    @Inject lateinit var roomSummaryUpdater: RoomSummaryUpdater
    @Inject lateinit var cryptoStore: IMXCryptoStore

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        var userList = params.updatedUserIds
        // Unfortunately we don't have much info on what did exactly changed (is it the cross signing keys of that user,
        // or a new device?) So we check all again :/

        Timber.d("## CrossSigning - Updating trust for $userList")

        // First we check that the users MSK are trusted by mine
        // After that we check the trust chain for each devices of each users
        Realm.getInstance(realmConfiguration).use { realm ->
            realm.executeTransaction {
                // By mapping here to model, this object is not live
                // I should update it if needed
                var myCrossSigningInfo = realm.where(CrossSigningInfoEntity::class.java)
                        .equalTo(CrossSigningInfoEntityFields.USER_ID, myUserId)
                        .findFirst()?.let { mapCrossSigningInfoEntity(it) }

                var myTrustResult: UserTrustResult? = null

                if (userList.contains(myUserId)) {
                    Timber.d("## CrossSigning - Clear all trust as a change on my user was detected")
                    // i am in the list.. but i don't know exactly the delta of change :/
                    // If it's my cross signing keys we should refresh all trust
                    // do it anyway ?
                    userList = realm.where(CrossSigningInfoEntity::class.java)
                            .findAll().mapNotNull { it.userId }
                    Timber.d("## CrossSigning - Updating trust for all $userList")

                    // check right now my keys and mark it as trusted as other trust depends on it
                    val myDevices = realm.where<UserEntity>()
                            .equalTo(UserEntityFields.USER_ID, myUserId)
                            .findFirst()
                            ?.devices
                            ?.map { deviceInfo ->
                                CryptoMapper.mapToModel(deviceInfo)
                            }
                    myTrustResult = crossSigningService.checkSelfTrust(myCrossSigningInfo, myDevices).also {
                        updateCrossSigningKeysTrust(realm, myUserId, it.isVerified())
                        // update model reference
                        myCrossSigningInfo = realm.where(CrossSigningInfoEntity::class.java)
                                .equalTo(CrossSigningInfoEntityFields.USER_ID, myUserId)
                                .findFirst()?.let { mapCrossSigningInfoEntity(it) }
                    }
                }

                val otherInfos = userList.map {
                    it to realm.where(CrossSigningInfoEntity::class.java)
                            .equalTo(CrossSigningInfoEntityFields.USER_ID, it)
                            .findFirst()?.let { mapCrossSigningInfoEntity(it) }
                }
                        .toMap()

                val trusts = otherInfos.map { infoEntry ->
                    infoEntry.key to when (infoEntry.key) {
                        myUserId -> myTrustResult
                        else     -> {
                            crossSigningService.checkOtherMSKTrusted(myCrossSigningInfo, infoEntry.value).also {
                                Timber.d("## CrossSigning - user:${infoEntry.key} result:$it")
                            }
                        }
                    }
                }.toMap()

                // TODO! if it's me and my keys has changed... I have to reset trust for everyone!
                // i have all the new trusts, update DB
                trusts.forEach {
                    val verified = it.value?.isVerified() == true
                    updateCrossSigningKeysTrust(realm, it.key, verified)
                }

                // Ok so now we have to check device trust for all these users..
                Timber.v("## CrossSigning - Updating devices cross trust users ${trusts.keys}")
                trusts.keys.forEach {
                    val devicesEntities = realm.where<UserEntity>()
                            .equalTo(UserEntityFields.USER_ID, it)
                            .findFirst()
                            ?.devices

                    val trustMap = devicesEntities?.map { device ->
                        // get up to date from DB has could have been updated
                        val otherInfo = realm.where(CrossSigningInfoEntity::class.java)
                                .equalTo(CrossSigningInfoEntityFields.USER_ID, it)
                                .findFirst()?.let { mapCrossSigningInfoEntity(it) }
                        device to crossSigningService.checkDeviceTrust(myCrossSigningInfo, otherInfo, CryptoMapper.mapToModel(device))
                    }?.toMap()

                    // Update trust if needed
                    devicesEntities?.forEach { device ->
                        val crossSignedVerified = trustMap?.get(device)?.isCrossSignedVerified()
                        Timber.d("## CrossSigning - Trust for ${device.userId}|${device.deviceId} : cross verified: ${trustMap?.get(device)}")
                        if (device.trustLevelEntity?.crossSignedVerified != crossSignedVerified) {
                            Timber.d("## CrossSigning - Trust change detected for ${device.userId}|${device.deviceId} : cross verified: $crossSignedVerified")
                            // need to save
                            val trustEntity = device.trustLevelEntity
                            if (trustEntity == null) {
                                realm.createObject(TrustLevelEntity::class.java).let {
                                    it.locallyVerified = false
                                    it.crossSignedVerified = crossSignedVerified
                                    device.trustLevelEntity = it
                                }
                            } else {
                                trustEntity.crossSignedVerified = crossSignedVerified
                            }
                        }
                    }
                }
            }
        }

        // So Cross Signing keys trust is updated, device trust is updated
        // We can now update room shields? in the session DB?

        Timber.d("## CrossSigning - Updating shields for impacted rooms...")
        Realm.getInstance(sessionRealmConfiguration).use { it ->
            it.executeTransaction { realm ->
                val distinctRoomIds = realm.where(RoomMemberSummaryEntity::class.java)
                        .`in`(RoomMemberSummaryEntityFields.USER_ID, userList.toTypedArray())
                        .distinct(RoomMemberSummaryEntityFields.ROOM_ID)
                        .findAll()
                        .map { it.roomId }
                Timber.d("## CrossSigning -  ... impacted rooms $distinctRoomIds")
                distinctRoomIds.forEach { roomId ->
                    val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                    if (roomSummary?.isEncrypted == true) {
                        Timber.d("## CrossSigning - Check shield state for room $roomId")
                        val allActiveRoomMembers = RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
                        try {
                            val updatedTrust = computeRoomShield(allActiveRoomMembers, roomSummary)
                            if (roomSummary.roomEncryptionTrustLevel != updatedTrust) {
                                Timber.d("## CrossSigning - Shield change detected for $roomId -> $updatedTrust")
                                roomSummary.roomEncryptionTrustLevel = updatedTrust
                            }
                        } catch (failure: Throwable) {
                            Timber.e(failure)
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    private fun updateCrossSigningKeysTrust(realm: Realm, userId: String, verified: Boolean) {
        val xInfoEntity = realm.where(CrossSigningInfoEntity::class.java)
                .equalTo(CrossSigningInfoEntityFields.USER_ID, userId)
                .findFirst()
        xInfoEntity?.crossSigningKeys?.forEach { info ->
            // optimization to avoid trigger updates when there is no change..
            if (info.trustLevelEntity?.isVerified() != verified) {
                Timber.d("## CrossSigning - Trust change for $userId : $verified")
                val level = info.trustLevelEntity
                if (level == null) {
                    val newLevel = realm.createObject(TrustLevelEntity::class.java)
                    newLevel.locallyVerified = verified
                    newLevel.crossSignedVerified = verified
                    info.trustLevelEntity = newLevel
                } else {
                    level.locallyVerified = verified
                    level.crossSignedVerified = verified
                }
            }
        }
    }

    private fun computeRoomShield(activeMemberUserIds: List<String>, roomSummaryEntity: RoomSummaryEntity): RoomEncryptionTrustLevel {
        Timber.d("## CrossSigning - computeRoomShield ${roomSummaryEntity.roomId} -> $activeMemberUserIds")
        // The set of “all users” depends on the type of room:
        // For regular / topic rooms which have more than 2 members (including yourself) are considered when decorating a room
        // For 1:1 and group DM rooms, all other users (i.e. excluding yourself) are considered when decorating a room
        val listToCheck = if (roomSummaryEntity.isDirect || activeMemberUserIds.size <= 2) {
            activeMemberUserIds.filter { it != myUserId }
        } else {
            activeMemberUserIds
        }

        val allTrustedUserIds = listToCheck
                .filter { userId ->
                    Realm.getInstance(realmConfiguration).use {
                        it.where(CrossSigningInfoEntity::class.java)
                                .equalTo(CrossSigningInfoEntityFields.USER_ID, userId)
                                .findFirst()?.let { mapCrossSigningInfoEntity(it) }?.isTrusted() == true
                    }
                }
        val myCrossKeys = Realm.getInstance(realmConfiguration).use {
            it.where(CrossSigningInfoEntity::class.java)
                    .equalTo(CrossSigningInfoEntityFields.USER_ID, myUserId)
                    .findFirst()?.let { mapCrossSigningInfoEntity(it) }
        }

        return if (allTrustedUserIds.isEmpty()) {
            RoomEncryptionTrustLevel.Default
        } else {
            // If one of the verified user as an untrusted device -> warning
            // If all devices of all verified users are trusted -> green
            // else -> black
            allTrustedUserIds
                    .mapNotNull { uid ->
                        Realm.getInstance(realmConfiguration).use {
                            it.where<UserEntity>()
                                    .equalTo(UserEntityFields.USER_ID, uid)
                                    .findFirst()
                                    ?.devices
                                    ?.map {
                                        CryptoMapper.mapToModel(it)
                                    }
                        }
                    }
                    .flatten()
                    .let { allDevices ->
                        Timber.v("## CrossSigning - computeRoomShield ${roomSummaryEntity.roomId} devices ${allDevices.map { it.deviceId }}")
                        if (myCrossKeys != null) {
                            allDevices.any { !it.trustLevel?.crossSigningVerified.orFalse() }
                        } else {
                            // Legacy method
                            allDevices.any { !it.isVerified }
                        }
                    }
                    .let { hasWarning ->
                        if (hasWarning) {
                            RoomEncryptionTrustLevel.Warning
                        } else {
                            if (listToCheck.size == allTrustedUserIds.size) {
                                // all users are trusted and all devices are verified
                                RoomEncryptionTrustLevel.Trusted
                            } else {
                                RoomEncryptionTrustLevel.Default
                            }
                        }
                    }
        }
    }

    private fun mapCrossSigningInfoEntity(xsignInfo: CrossSigningInfoEntity): MXCrossSigningInfo {
        val userId = xsignInfo.userId ?: ""
        return MXCrossSigningInfo(
                userId = userId,
                crossSigningKeys = xsignInfo.crossSigningKeys.mapNotNull {
                    crossSigningKeysMapper.map(userId, it)
                }
        )
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
