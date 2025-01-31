/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        // setting to enable or disable key gossiping
        var globalEnableKeyGossiping: Boolean = true,

        // MSC3061: Sharing room keys for past messages
        // If set to true key history will be shared to invited users with respect to room setting
        var enableKeyForwardingOnInvite: Boolean = false,

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
