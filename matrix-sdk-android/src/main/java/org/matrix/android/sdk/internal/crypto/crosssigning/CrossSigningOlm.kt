/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.crosssigning

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmPkSigning
import org.matrix.olm.OlmUtility
import javax.inject.Inject

/**
 * Holds the OlmPkSigning for cross signing.
 * Can be injected without having to get the full cross signing service
 */
@SessionScope
internal class CrossSigningOlm @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
) {

    enum class KeyType {
        SELF,
        USER,
        MASTER
    }

    var olmUtility: OlmUtility = OlmUtility()

    var masterPkSigning: OlmPkSigning? = null
    var userPkSigning: OlmPkSigning? = null
    var selfSigningPkSigning: OlmPkSigning? = null

    fun release() {
        olmUtility.releaseUtility()
        listOf(masterPkSigning, userPkSigning, selfSigningPkSigning).forEach { it?.releaseSigning() }
    }

    fun signObject(type: KeyType, strToSign: String): Map<String, String> {
        val myKeys = cryptoStore.getMyCrossSigningInfo()
        val pubKey = when (type) {
            KeyType.SELF -> myKeys?.selfSigningKey()
            KeyType.USER -> myKeys?.userKey()
            KeyType.MASTER -> myKeys?.masterKey()
        }?.unpaddedBase64PublicKey
        val pkSigning = when (type) {
            KeyType.SELF -> selfSigningPkSigning
            KeyType.USER -> userPkSigning
            KeyType.MASTER -> masterPkSigning
        }
        if (pubKey == null || pkSigning == null) {
            throw Throwable("Cannot sign from this account, public and/or privateKey Unknown $type|$pkSigning")
        }
        val signature = pkSigning.sign(strToSign)
        return mapOf(
                "ed25519:$pubKey" to signature
        )
    }

    fun verifySignature(type: KeyType, signable: JsonDict, signatures: Map<String, Map<String, String>>) {
        val myKeys = cryptoStore.getMyCrossSigningInfo()
                ?: throw NoSuchElementException("Cross Signing not configured")
        val myUserID = myKeys.userId
        val pubKey = when (type) {
            KeyType.SELF -> myKeys.selfSigningKey()
            KeyType.USER -> myKeys.userKey()
            KeyType.MASTER -> myKeys.masterKey()
        }?.unpaddedBase64PublicKey ?: throw NoSuchElementException("Cross Signing not configured")
        val signaturesMadeByMyKey = signatures[myUserID] // Signatures made by me
                ?.get("ed25519:$pubKey")

        if (signaturesMadeByMyKey.isNullOrBlank()) {
            throw IllegalArgumentException("Not signed with my key $type")
        }

        // Check that Alice USK signature of Bob MSK is valid
        olmUtility.verifyEd25519Signature(signaturesMadeByMyKey, pubKey, JsonCanonicalizer.getCanonicalJson(Map::class.java, signable))
    }
}
