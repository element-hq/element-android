/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.crypto.verification.safeValueOf
import org.matrix.android.sdk.internal.crypto.verification.getEmojiForCode
import timber.log.Timber
import uniffi.olm.CryptoStoreErrorException
import uniffi.olm.OlmMachine
import uniffi.olm.OutgoingVerificationRequest
import uniffi.olm.Sas
import uniffi.olm.Verification

internal class SasVerification(
        private val machine: OlmMachine,
        private var inner: Sas,
        private val sender: RequestSender,
        private val listeners: ArrayList<VerificationService.Listener>,
) :
        SasVerificationTransaction {
    private val uiHandler = Handler(Looper.getMainLooper())

    private fun dispatchTxUpdated() {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.transactionUpdated(this)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

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

    override val isIncoming: Boolean
        get() = !this.inner.weStarted

    override var otherDeviceId: String?
        get() = this.inner.otherDeviceId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
        }

    override val otherUserId: String = this.inner.otherUserId

    override var state: VerificationTxState
        get() {
            refreshData()
            return when {
                this.inner.isCancelled     -> {
                    val cancelCode = safeValueOf(this.inner.cancelCode)
                    val byMe = this.inner.cancelledByUs ?: false
                    VerificationTxState.Cancelled(cancelCode, byMe)
                }
                this.inner.isDone          -> VerificationTxState.Verified
                this.inner.haveWeConfirmed -> VerificationTxState.ShortCodeAccepted
                this.inner.canBePresented  -> VerificationTxState.ShortCodeReady
                this.inner.hasBeenAccepted -> VerificationTxState.Accepted
                else                       -> VerificationTxState.OnStarted
            }
        }
        @Suppress("UNUSED_PARAMETER")
        set(v) {
        }

    override val transactionId: String
        get() = this.inner.flowId

    override fun cancel() {
        this.cancelHelper(CancelCode.User)
    }

    override fun cancel(code: CancelCode) {
        this.cancelHelper(code)
    }

    override fun shortCodeDoesNotMatch() {
        this.cancelHelper(CancelCode.MismatchedSas)
    }

    override fun isToDeviceTransport(): Boolean = this.inner.roomId == null

    override fun supportsDecimal(): Boolean {
        // This is ignored anyways, throw it away?
        // The spec also mandates that devices support
        // at least decimal and the rust-sdk cancels if
        // devices don't support it
        return true
    }

    override fun supportsEmoji(): Boolean {
        refreshData()
        return this.inner.supportsEmoji
    }

    override fun userHasVerifiedShortCode() {
        val request = runBlocking { confirm() } ?: return
        sendRequest(request)
    }

    override fun acceptVerification() {
        runBlocking { accept() }
    }

    suspend fun accept() {
        val request = this.machine.acceptSasVerification(this.inner.otherUserId, inner.flowId)

        if (request != null) {
            this.sender.sendVerificationRequest(request)
            refreshData()
            dispatchTxUpdated()
        }
    }

    @Throws(CryptoStoreErrorException::class)
    suspend fun confirm(): OutgoingVerificationRequest? =
            withContext(Dispatchers.IO) {
                machine.confirmVerification(inner.otherUserId, inner.flowId)
            }

    fun cancelHelper(code: CancelCode) {
        val request = this.machine.cancelVerification(this.inner.otherUserId, inner.flowId, code.value)

        if (request != null) {
            sendRequest(request)
        }
    }

    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        val emojiIndex = this.machine.getEmojiIndex(this.inner.otherUserId, this.inner.flowId)

        return emojiIndex?.map { getEmojiForCode(it) } ?: listOf()
    }

    override fun getDecimalCodeRepresentation(): String {
        val decimals = this.machine.getDecimals(this.inner.otherUserId, this.inner.flowId)

        return decimals?.joinToString(" ") ?: ""
    }

    fun sendRequest(request: OutgoingVerificationRequest) {
        runBlocking {
            sender.sendVerificationRequest(request)
        }

        refreshData()
        dispatchTxUpdated()
    }
}
