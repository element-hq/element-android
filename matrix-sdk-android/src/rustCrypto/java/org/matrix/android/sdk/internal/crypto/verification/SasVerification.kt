/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import uniffi.olm.CryptoStoreException
import uniffi.olm.Sas
import uniffi.olm.Verification

/** Class representing a short auth string verification flow */
internal class SasVerification @AssistedInject constructor(
        @Assisted private var inner: Sas,
        private val olmMachine: OlmMachine,
        private val sender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationListenersHolder: VerificationListenersHolder
) :
        SasVerificationTransaction {

    @AssistedFactory
    interface Factory {
        fun create(inner: Sas): SasVerification
    }

    private val innerMachine = olmMachine.inner()

    private fun dispatchTxUpdated() {
        refreshData()
        verificationListenersHolder.dispatchTxUpdated(this)
    }

    /** The user ID of the other user that is participating in this verification flow */
    override val otherUserId: String = inner.otherUserId

    /** Get the device id of the other user's device participating in this verification flow */
    override var otherDeviceId: String?
        get() = inner.otherDeviceId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    /** Did the other side initiate this verification flow */
    override val isIncoming: Boolean
        get() = !inner.weStarted

    override var state: VerificationTxState
        get() {
            refreshData()
            val cancelInfo = inner.cancelInfo

            return when {
                cancelInfo != null    -> {
                    val cancelCode = safeValueOf(cancelInfo.cancelCode)
                    VerificationTxState.Cancelled(cancelCode, cancelInfo.cancelledByUs)
                }
                inner.isDone          -> VerificationTxState.Verified
                inner.haveWeConfirmed -> VerificationTxState.SasMacSent
                inner.canBePresented  -> VerificationTxState.SasShortCodeReady
                inner.hasBeenAccepted -> VerificationTxState.SasAccepted
                else                  -> VerificationTxState.SasStarted
            }
        }
        @Suppress("UNUSED_PARAMETER")
        set(v) {
        }

    /** Get the unique id of this verification */
    override val transactionId: String
        get() = inner.flowId

    /** Cancel the verification flow
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to m.user.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     * */
    override suspend fun cancel() {
        cancelHelper(CancelCode.User)
    }

    /** Cancel the verification flow
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to the given CancelCode.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     *
     * @param code The cancel code that should be given as the reason for the cancellation.
     * */
    override suspend fun cancel(code: CancelCode) {
        cancelHelper(code)
    }

    /** Cancel the verification flow
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to the m.mismatched_sas cancel code.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     */
    override suspend fun shortCodeDoesNotMatch() {
        cancelHelper(CancelCode.MismatchedSas)
    }

    /** Is this verification happening over to-device messages */
    override fun isToDeviceTransport(): Boolean = inner.roomId == null

    /** Does the verification flow support showing emojis as the short auth string */
    override fun supportsEmoji(): Boolean {
        refreshData()
        return inner.supportsEmoji
    }

    /** Confirm that the short authentication code matches on both sides
     *
     * This sends a m.key.verification.mac event out, the verification isn't yet
     * done, we still need to receive such an event from the other side if we haven't
     * already done so.
     *
     * This method is a noop if we're not yet in a presentable state, i.e. we didn't receive
     * a m.key.verification.key event from the other side or we're cancelled.
     */
    override suspend fun userHasVerifiedShortCode() {
        confirm()
    }

    /** Accept the verification flow, signaling the other side that we do want to verify
     *
     * This sends a m.key.verification.accept event out that is a response to a
     * m.key.verification.start event from the other side.
     *
     * This method is a noop if we send the start event out or if the verification has already
     * been accepted.
     */
    override suspend fun acceptVerification() {
        accept()
    }

    /** Get the decimal representation of the short auth string
     *
     * @return A string of three space delimited numbers that
     * represent the short auth string or an empty string if we're not yet
     * in a presentable state.
     */
    override fun getDecimalCodeRepresentation(): String {
        val decimals = innerMachine.getDecimals(inner.otherUserId, inner.flowId)

        return decimals?.joinToString(" ") ?: ""
    }

    /** Get the emoji representation of the short auth string
     *
     * @return A list of 7 EmojiRepresentation objects that represent the
     * short auth string or an empty list if we're not yet in a presentable
     * state.
     */
    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        val emojiIndex = innerMachine.getEmojiIndex(inner.otherUserId, inner.flowId)

        return emojiIndex?.map { getEmojiForCode(it) } ?: listOf()
    }

    internal suspend fun accept() {
        val request = innerMachine.acceptSasVerification(inner.otherUserId, inner.flowId) ?: return
        dispatchTxUpdated()
        try {
            sender.sendVerificationRequest(request)
        } catch (failure: Throwable) {
            cancelHelper(CancelCode.UserError)
        }
    }

    @Throws(CryptoStoreException::class)
    private suspend fun confirm() {
        val result = withContext(coroutineDispatchers.io) {
            innerMachine.confirmVerification(inner.otherUserId, inner.flowId)
        } ?: return

        dispatchTxUpdated()
        try {
            for (verificationRequest in result.requests) {
                sender.sendVerificationRequest(verificationRequest)
            }
            val signatureRequest = result.signatureRequest
            if (signatureRequest != null) {
                sender.sendSignatureUpload(signatureRequest)
            }
        } catch (failure: Throwable) {
            cancelHelper(CancelCode.UserError)
        }
    }

    private suspend fun cancelHelper(code: CancelCode) = withContext(NonCancellable) {
        val request = innerMachine.cancelVerification(inner.otherUserId, inner.flowId, code.value) ?: return@withContext
        dispatchTxUpdated()
        tryOrNull("Fail to send cancel request") {
            sender.sendVerificationRequest(request, retryCount = Int.MAX_VALUE)
        }
    }

    /** Fetch fresh data from the Rust side for our verification flow */
    private fun refreshData() {
        when (val verification = innerMachine.getVerification(inner.otherUserId, inner.flowId)) {
            is Verification.SasV1 -> {
                inner = verification.sas
            }
            else                  -> {
            }
        }

        return
    }

    override fun toString(): String {
        return "SasVerification(" +
                "otherUserId='$otherUserId', " +
                "otherDeviceId=$otherDeviceId, " +
                "isIncoming=$isIncoming, " +
                "state=$state, " +
                "transactionId='$transactionId')"
    }
}
