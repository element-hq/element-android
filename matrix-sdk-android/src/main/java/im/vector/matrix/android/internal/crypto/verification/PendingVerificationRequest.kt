/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationRequestContent
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import im.vector.matrix.android.internal.crypto.model.rest.VERIFICATION_METHOD_SCAN
import java.util.*

/**
 * Stores current pending verification requests
 */
data class PendingVerificationRequest(
        val ageLocalTs: Long,
        val isIncoming: Boolean = false,
        val localID: String = UUID.randomUUID().toString(),
        val otherUserId: String,
        val roomId: String?,
        val transactionId: String? = null,
        val requestInfo: MessageVerificationRequestContent? = null,
        val readyInfo: VerificationInfoReady? = null,
        val cancelConclusion: CancelCode? = null,
        val isSuccessful: Boolean = false,
        val handledByOtherSession: Boolean = false

) {
    val isReady: Boolean = readyInfo != null
    val isSent: Boolean = transactionId != null

    val isFinished: Boolean = isSuccessful || cancelConclusion != null

    fun hasMethod(method: VerificationMethod): Boolean? {
        return when (method) {
            VerificationMethod.SAS  -> readyInfo?.methods?.contains(VERIFICATION_METHOD_SAS)
            VerificationMethod.SCAN -> readyInfo?.methods?.contains(VERIFICATION_METHOD_SCAN)
        }
    }
}
