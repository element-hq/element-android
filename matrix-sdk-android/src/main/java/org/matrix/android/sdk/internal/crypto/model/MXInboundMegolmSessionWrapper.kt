/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.olm.OlmInboundGroupSession
import timber.log.Timber

data class MXInboundMegolmSessionWrapper(
        // olm object
        val session: OlmInboundGroupSession,
        // data about the session
        val sessionData: InboundGroupSessionData
) {
    // shortcut
    val roomId = sessionData.roomId
    val senderKey = sessionData.senderKey
    val safeSessionId = tryOrNull("Fail to get megolm session Id") { session.sessionIdentifier() }

    /**
     * Export the inbound group session keys.
     * @param index the index to export. If null, the first known index will be used
     * @return the inbound group session as MegolmSessionData if the operation succeeds
     */
    internal fun exportKeys(index: Long? = null): MegolmSessionData? {
        return try {
            val keysClaimed = sessionData.keysClaimed ?: return null
            val wantedIndex = index ?: session.firstKnownIndex

            MegolmSessionData(
                    senderClaimedEd25519Key = sessionData.keysClaimed?.get("ed25519"),
                    forwardingCurve25519KeyChain = sessionData.forwardingCurve25519KeyChain?.toList().orEmpty(),
                    sessionKey = session.export(wantedIndex),
                    senderClaimedKeys = keysClaimed,
                    roomId = sessionData.roomId,
                    sessionId = session.sessionIdentifier(),
                    senderKey = senderKey,
                    algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                    sharedHistory = sessionData.sharedHistory
            )
        } catch (e: Exception) {
            Timber.e(e, "## Failed to export megolm : sessionID ${tryOrNull { session.sessionIdentifier() }} failed")
            null
        }
    }

    companion object {

        /**
         * @exportFormat true if the megolm keys are in export format
         *    (ie, they lack an ed25519 signature)
         */
        @Throws
        internal fun newFromMegolmData(megolmSessionData: MegolmSessionData, exportFormat: Boolean): MXInboundMegolmSessionWrapper {
            val exportedKey = megolmSessionData.sessionKey ?: throw IllegalArgumentException("key data not found")
            val inboundSession = if (exportFormat) {
                OlmInboundGroupSession.importSession(exportedKey)
            } else {
                OlmInboundGroupSession(exportedKey)
            }
                    .also {
                        if (it.sessionIdentifier() != megolmSessionData.sessionId) {
                            it.releaseSession()
                            throw IllegalStateException("Mismatched group session Id")
                        }
                    }
            val data = InboundGroupSessionData(
                    roomId = megolmSessionData.roomId,
                    senderKey = megolmSessionData.senderKey,
                    keysClaimed = megolmSessionData.senderClaimedKeys,
                    forwardingCurve25519KeyChain = megolmSessionData.forwardingCurve25519KeyChain,
                    sharedHistory = megolmSessionData.sharedHistory,
                    trusted = false
            )

            return MXInboundMegolmSessionWrapper(
                    inboundSession,
                    data
            )
        }
    }
}
