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
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.rustcomponents.sdk.crypto.CryptoStoreException
import org.matrix.rustcomponents.sdk.crypto.Sas
import org.matrix.rustcomponents.sdk.crypto.SasListener
import org.matrix.rustcomponents.sdk.crypto.SasState

/** Class representing a short auth string verification flow. */
internal class SasVerification @AssistedInject constructor(
        @Assisted private var inner: Sas,
//        private val olmMachine: OlmMachine,
        private val sender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val verificationListenersHolder: VerificationListenersHolder,
) :
        SasVerificationTransaction, SasListener {

    init {
        inner.setChangesListener(this)
    }

    var innerState: SasState = SasState.Started

    @AssistedFactory
    interface Factory {
        fun create(inner: Sas): SasVerification
    }

    /** The user ID of the other user that is participating in this verification flow. */
    override val otherUserId: String = inner.otherUserId()

    /** Get the device id of the other user's device participating in this verification flow. */
    override val otherDeviceId: String
        get() = inner.otherDeviceId()

    /** Did the other side initiate this verification flow. */
    override val isIncoming: Boolean
        get() = !inner.weStarted()

    private var decimals: List<Int>? = null
    private var emojis: List<Int>? = null

    override fun state(): SasTransactionState {
        return when (val state = innerState) {
            SasState.Started -> SasTransactionState.SasStarted
            SasState.Accepted -> SasTransactionState.SasAccepted
            is SasState.KeysExchanged -> {
                this.decimals = state.decimals
                this.emojis = state.emojis
                SasTransactionState.SasShortCodeReady
            }
            SasState.Confirmed -> SasTransactionState.SasMacSent
            SasState.Done -> SasTransactionState.Done(true)
            is SasState.Cancelled -> SasTransactionState.Cancelled(safeValueOf(state.cancelInfo.cancelCode), state.cancelInfo.cancelledByUs)
        }
    }

    /** Get the unique id of this verification. */
    override val transactionId: String
        get() = inner.flowId()

    /** Cancel the verification flow.
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

    /** Cancel the verification flow.
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

    /** Cancel the verification flow.
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

    override val method: VerificationMethod
        get() = VerificationMethod.QR_CODE_SCAN

    /** Is this verification happening over to-device messages. */
    override fun isToDeviceTransport(): Boolean = inner.roomId() == null

//    /** Does the verification flow support showing emojis as the short auth string */
//    override fun supportsEmoji(): Boolean {
//        return inner.supportsEmoji()
//    }

    /** Confirm that the short authentication code matches on both sides.
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

    /** Accept the verification flow, signaling the other side that we do want to verify.
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

    /** Get the decimal representation of the short auth string.
     *
     * @return A string of three space delimited numbers that
     * represent the short auth string or an empty string if we're not yet
     * in a presentable state.
     */
    override fun getDecimalCodeRepresentation(): String {
        return decimals?.joinToString(" ") ?: ""
    }

    /** Get the emoji representation of the short auth string.
     *
     * @return A list of 7 EmojiRepresentation objects that represent the
     * short auth string or an empty list if we're not yet in a presentable
     * state.
     */
    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        return emojis?.map { getEmojiForCode(it) } ?: listOf()
    }

    internal suspend fun accept() {
        val request = inner.accept() ?: return Unit.also {
            // TODO should throw here?
        }
        try {
            sender.sendVerificationRequest(request)
        } catch (failure: Throwable) {
            cancelHelper(CancelCode.UserError)
        }
    }

    @Throws(CryptoStoreException::class)
    private suspend fun confirm() {
        val result = withContext(coroutineDispatchers.io) {
            inner.confirm()
        } ?: return
        try {
            for (verificationRequest in result.requests) {
                sender.sendVerificationRequest(verificationRequest)
                verificationListenersHolder.dispatchTxUpdated(this)
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
        val request = inner.cancel(code.value) ?: return@withContext
        tryOrNull("Fail to send cancel request") {
            sender.sendVerificationRequest(request, retryCount = Int.MAX_VALUE)
        }
        verificationListenersHolder.dispatchTxUpdated(this@SasVerification)
    }

    override fun onChange(state: SasState) {
        innerState = state
        verificationListenersHolder.dispatchTxUpdated(this)
    }

    override fun toString(): String {
        return "SasVerification(" +
                "otherUserId='$otherUserId', " +
                "otherDeviceId=$otherDeviceId, " +
                "isIncoming=$isIncoming, " +
                "state=${state()}, " +
                "transactionId='$transactionId')"
    }
}
