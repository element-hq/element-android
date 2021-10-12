/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.verification.UpdateDispatcher
import org.matrix.android.sdk.internal.crypto.verification.getEmojiForCode
import uniffi.olm.CryptoStoreErrorException
import uniffi.olm.OlmMachine
import uniffi.olm.Sas
import uniffi.olm.Verification

/** Class representing a short auth string verification flow */
internal class SasVerification(
        private val machine: OlmMachine,
        private var inner: Sas,
        private val sender: RequestSender,
        listeners: ArrayList<VerificationService.Listener>,
) :
        SasVerificationTransaction {
    private val dispatcher = UpdateDispatcher(listeners)

    private fun dispatchTxUpdated() {
        refreshData()
        this.dispatcher.dispatchTxUpdated(this)
    }

    /** The user ID of the other user that is participating in this verification flow */
    override val otherUserId: String = this.inner.otherUserId

    /** Get the device id of the other user's device participating in this verification flow */
    override var otherDeviceId: String?
        get() = this.inner.otherDeviceId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    /** Did the other side initiate this verification flow */
    override val isIncoming: Boolean
        get() = !this.inner.weStarted

    override var state: VerificationTxState
        get() {
            refreshData()
            val cancelInfo = this.inner.cancelInfo

            return when {
                cancelInfo != null    -> {
                    val cancelCode = safeValueOf(cancelInfo.cancelCode)
                    VerificationTxState.Cancelled(cancelCode, cancelInfo.cancelledByUs)
                }
                inner.isDone          -> VerificationTxState.Verified
                inner.haveWeConfirmed -> VerificationTxState.ShortCodeAccepted
                inner.canBePresented  -> VerificationTxState.ShortCodeReady
                inner.hasBeenAccepted -> VerificationTxState.Accepted
                else                  -> VerificationTxState.OnStarted
            }
        }
        @Suppress("UNUSED_PARAMETER")
        set(v) {
        }

    /** Get the unique id of this verification */
    override val transactionId: String
        get() = this.inner.flowId

    /** Cancel the verification flow
     *
     * This will send out a m.key.verification.cancel event with the cancel
     * code set to m.user.
     *
     * Cancelling the verification request will also cancel the parent VerificationRequest.
     *
     * The method turns into a noop, if the verification flow has already been cancelled.
     * */
    override fun cancel() {
        this.cancelHelper(CancelCode.User)
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
    override fun cancel(code: CancelCode) {
        this.cancelHelper(code)
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
    override fun shortCodeDoesNotMatch() {
        this.cancelHelper(CancelCode.MismatchedSas)
    }

    /** Is this verification happening over to-device messages */
    override fun isToDeviceTransport(): Boolean = this.inner.roomId == null

    /** Does the verification flow support showing decimals as the short auth string */
    override fun supportsDecimal(): Boolean {
        // This is ignored anyways, throw it away?
        // The spec also mandates that devices support at least decimal and
        // the rust-sdk cancels if devices don't support it
        return true
    }

    /** Does the verification flow support showing emojis as the short auth string */
    override fun supportsEmoji(): Boolean {
        refreshData()
        return this.inner.supportsEmoji
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
    override fun userHasVerifiedShortCode() {
        runBlocking { confirm() }
    }

    /** Accept the verification flow, signaling the other side that we do want to verify
     *
     * This sends a m.key.verification.accept event out that is a response to a
     * m.key.verification.start event from the other side.
     *
     * This method is a noop if we send the start event out or if the verification has already
     * been accepted.
     */
    override fun acceptVerification() {
        runBlocking { accept() }
    }

    /** Get the decimal representation of the short auth string
     *
     * @return A string of three space delimited numbers that
     * represent the short auth string or an empty string if we're not yet
     * in a presentable state.
     */
    override fun getDecimalCodeRepresentation(): String {
        val decimals = this.machine.getDecimals(this.inner.otherUserId, this.inner.flowId)

        return decimals?.joinToString(" ") ?: ""
    }

    /** Get the emoji representation of the short auth string
     *
     * @return A list of 7 EmojiRepresentation objects that represent the
     * short auth string or an empty list if we're not yet in a presentable
     * state.
     */
    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        val emojiIndex = this.machine.getEmojiIndex(this.inner.otherUserId, this.inner.flowId)

        return emojiIndex?.map { getEmojiForCode(it) } ?: listOf()
    }

    internal suspend fun accept() {
        val request = this.machine.acceptSasVerification(this.inner.otherUserId, inner.flowId)

        if (request != null) {
            this.sender.sendVerificationRequest(request)
            dispatchTxUpdated()
        }
    }

    @Throws(CryptoStoreErrorException::class)
    private suspend fun confirm() {
        val result = withContext(Dispatchers.IO) {
            machine.confirmVerification(inner.otherUserId, inner.flowId)
        }

        if (result != null) {
            this.sender.sendVerificationRequest(result.request)
            dispatchTxUpdated()

            val signatureRequest = result.signatureRequest

            if (signatureRequest != null) {
                this.sender.sendSignatureUpload(signatureRequest)
            }
        }
    }

    private fun cancelHelper(code: CancelCode) {
        val request = this.machine.cancelVerification(this.inner.otherUserId, inner.flowId, code.value)

        if (request != null) {
            runBlocking { sender.sendVerificationRequest(request) }
            dispatchTxUpdated()
        }
    }

    /** Fetch fresh data from the Rust side for our verification flow */
    private fun refreshData() {
        when (val verification = this.machine.getVerification(this.inner.otherUserId, this.inner.flowId)) {
            is Verification.SasV1 -> {
                this.inner = verification.sas
            }
            else                  -> {
            }
        }

        return
    }
}
