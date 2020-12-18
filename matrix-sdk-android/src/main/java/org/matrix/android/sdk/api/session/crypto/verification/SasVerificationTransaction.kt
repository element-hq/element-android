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

    fun supportsEmoji(): Boolean

    fun supportsDecimal(): Boolean

    fun getEmojiCodeRepresentation(): List<EmojiRepresentation>

    fun getDecimalCodeRepresentation(): String

    /**
     * To be called by the client when the user has verified that
     * both short codes do match
     */
    fun userHasVerifiedShortCode()

    fun shortCodeDoesNotMatch()
}
