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

package org.matrix.android.sdk.api.session.crypto.verification

interface SasVerificationTransaction : VerificationTransaction {

    companion object {
        const val SAS_MAC_SHA256_LONGKDF = "hmac-sha256"
        const val SAS_MAC_SHA256 = "hkdf-hmac-sha256"

        // Deprecated maybe removed later, use V2
        const val KEY_AGREEMENT_V1 = "curve25519"
        const val KEY_AGREEMENT_V2 = "curve25519-hkdf-sha256"

        // ordered by preferred order
        val KNOWN_AGREEMENT_PROTOCOLS = listOf(KEY_AGREEMENT_V2, KEY_AGREEMENT_V1)

        // ordered by preferred order
        val KNOWN_HASHES = listOf("sha256")

        // ordered by preferred order
        val KNOWN_MACS = listOf(SAS_MAC_SHA256, SAS_MAC_SHA256_LONGKDF)

        // older devices have limited support of emoji but SDK offers images for the 64 verification emojis
        // so always send that we support EMOJI
        val KNOWN_SHORT_CODES = listOf(SasMode.EMOJI, SasMode.DECIMAL)
    }

    fun state(): SasTransactionState

    override fun isSuccessful() = state() is SasTransactionState.Done

//    fun supportsEmoji(): Boolean

    fun getEmojiCodeRepresentation(): List<EmojiRepresentation>

    fun getDecimalCodeRepresentation(): String?

    /**
     * To be called by the client when the user has verified that
     * both short codes do match.
     */
    suspend fun userHasVerifiedShortCode()

    suspend fun acceptVerification()

    suspend fun shortCodeDoesNotMatch()
}
