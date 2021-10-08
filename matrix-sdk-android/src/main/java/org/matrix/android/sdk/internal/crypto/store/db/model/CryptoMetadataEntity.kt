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

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.olm.OlmAccount

internal open class CryptoMetadataEntity(
        // The current user id.
        @PrimaryKey var userId: String? = null,
        // The current device id.
        var deviceId: String? = null,
        // Serialized OlmAccount
        var olmAccountData: String? = null,
        // The sync token corresponding to the device list. // TODO?
        var deviceSyncToken: String? = null,
        // Settings for blacklisting unverified devices.
        var globalBlacklistUnverifiedDevices: Boolean = false,
        // The keys backup version currently used. Null means no backup.
        var backupVersion: String? = null,

        // The device keys has been sent to the homeserver
        var deviceKeysSentToServer: Boolean = false,

        var xSignMasterPrivateKey: String? = null,
        var xSignUserPrivateKey: String? = null,
        var xSignSelfSignedPrivateKey: String? = null,
        var keyBackupRecoveryKey: String? = null,
        var keyBackupRecoveryKeyVersion: String? = null

//        var crossSigningInfoEntity: CrossSigningInfoEntity? = null
) : RealmObject() {

    // Deserialize data
    fun getOlmAccount(): OlmAccount? {
        return deserializeFromRealm(olmAccountData)
    }

    // Serialize data
    fun putOlmAccount(olmAccount: OlmAccount?) {
        olmAccountData = serializeForRealm(olmAccount)
    }
}
