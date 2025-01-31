/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.olm.OlmInboundGroupSession
import timber.log.Timber
import java.io.Serializable

/**
 * This class adds more context to a OlmInboundGroupSession object.
 * This allows additional checks. The class implements Serializable so that the context can be stored.
 */
internal class OlmInboundGroupSessionWrapper : Serializable {

    // The associated olm inbound group session.
    var olmInboundGroupSession: OlmInboundGroupSession? = null

    // The room in which this session is used.
    var roomId: String? = null

    // The base64-encoded curve25519 key of the sender.
    var senderKey: String? = null

    // Other keys the sender claims.
    var keysClaimed: Map<String, String>? = null

    // Devices which forwarded this session to us (normally empty).
    var forwardingCurve25519KeyChain: List<String>? = ArrayList()

    /**
     * @return the first known message index
     */
    val firstKnownIndex: Long?
        get() {
            if (null != olmInboundGroupSession) {
                try {
                    return olmInboundGroupSession!!.firstKnownIndex
                } catch (e: Exception) {
                    Timber.e(e, "## getFirstKnownIndex() : getFirstKnownIndex failed")
                }
            }

            return null
        }

    /**
     * Constructor.
     *
     * @param sessionKey the session key
     * @param isImported true if it is an imported session key
     */
    constructor(sessionKey: String, isImported: Boolean) {
        try {
            if (!isImported) {
                olmInboundGroupSession = OlmInboundGroupSession(sessionKey)
            } else {
                olmInboundGroupSession = OlmInboundGroupSession.importSession(sessionKey)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot create")
        }
    }

    /**
     * Create a new instance from the provided keys map.
     *
     * @param megolmSessionData the megolm session data
     * @throws Exception if the data are invalid
     */
    @Throws(Exception::class)
    constructor(megolmSessionData: MegolmSessionData) {
        try {
            olmInboundGroupSession = OlmInboundGroupSession.importSession(megolmSessionData.sessionKey!!)

            if (olmInboundGroupSession!!.sessionIdentifier() != megolmSessionData.sessionId) {
                throw Exception("Mismatched group session Id")
            }

            senderKey = megolmSessionData.senderKey
            keysClaimed = megolmSessionData.senderClaimedKeys
            roomId = megolmSessionData.roomId
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    /**
     * Export the inbound group session keys.
     *
     * @return the inbound group session as MegolmSessionData if the operation succeeds
     */
    fun exportKeys(): MegolmSessionData? {
        return try {
            if (null == forwardingCurve25519KeyChain) {
                forwardingCurve25519KeyChain = ArrayList()
            }

            if (keysClaimed == null) {
                return null
            }

            MegolmSessionData(
                    senderClaimedEd25519Key = keysClaimed?.get("ed25519"),
                    forwardingCurve25519KeyChain = ArrayList(forwardingCurve25519KeyChain!!),
                    senderKey = senderKey,
                    senderClaimedKeys = keysClaimed,
                    roomId = roomId,
                    sessionId = olmInboundGroupSession!!.sessionIdentifier(),
                    sessionKey = olmInboundGroupSession!!.export(olmInboundGroupSession!!.firstKnownIndex),
                    algorithm = MXCRYPTO_ALGORITHM_MEGOLM
            )
        } catch (e: Exception) {
            Timber.e(e, "## export() : senderKey $senderKey failed")
            null
        }
    }

    /**
     * Export the session for a message index.
     *
     * @param messageIndex the message index
     * @return the exported data
     */
    fun exportSession(messageIndex: Long): String? {
        if (null != olmInboundGroupSession) {
            try {
                return olmInboundGroupSession!!.export(messageIndex)
            } catch (e: Exception) {
                Timber.e(e, "## exportSession() : export failed")
            }
        }

        return null
    }
}
