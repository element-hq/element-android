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
import org.matrix.android.sdk.internal.crypto.verification.getEmojiForCode
import timber.log.Timber
import uniffi.olm.CryptoStoreErrorException
import uniffi.olm.OlmMachine
import uniffi.olm.OutgoingVerificationRequest
import uniffi.olm.Sas

internal class SasVerification(
        private val machine: OlmMachine,
        private var inner: Sas,
        private val sender: RequestSender,
        private val listeners: ArrayList<VerificationService.Listener>,
        ) :
        SasVerificationTransaction {
    private val uiHandler = Handler(Looper.getMainLooper())
    private var stateField: VerificationTxState = VerificationTxState.OnStarted

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
        val sas = this.machine.getVerification(this.inner.otherUserId, this.inner.flowId)

        if (sas != null) {
            this.inner = sas
        }

        return
    }

    override val isIncoming: Boolean
        get() {
            return false
        }

    override var otherDeviceId: String?
        get() {
            return this.inner.otherDeviceId
        }
        set(value) {
            if (value != null) {
                this.inner.otherDeviceId = value
            }
        }

    override val otherUserId: String
        get() {
            return this.inner.otherUserId
        }

    override var state: VerificationTxState
        get() {
            refreshData()
            return when {
                this.inner.isDone         -> VerificationTxState.Verified
                this.inner.haveWeConfirmed -> VerificationTxState.ShortCodeAccepted
                this.inner.canBePresented -> VerificationTxState.ShortCodeReady
                this.inner.isCancelled    -> {
                    val rustCancelCode = this.inner.cancelCode
                    val cancelCode =
                            if (rustCancelCode != null) {
                                toCancelCode(rustCancelCode)
                            } else {
                                CancelCode.UnexpectedMessage
                            }
                    // TODO get byMe from the rust side
                    VerificationTxState.Cancelled(cancelCode, false)
                }
                else                      -> {
                    VerificationTxState.Started
                }
            }
        }
        set(v) {
            this.stateField = v
        }

    override val transactionId: String
        get() {
            return this.inner.flowId
        }

    override fun cancel() {
        TODO()
    }

    override fun cancel(code: CancelCode) {
        TODO()
    }

    override fun shortCodeDoesNotMatch() {
        TODO()
    }

    override fun isToDeviceTransport(): Boolean {
        return false
    }

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
        runBlocking {
            when (val request = confirm()) {
                is OutgoingVerificationRequest.ToDevice -> {
                    sender.sendToDevice(request.eventType, request.body)
            }

                else                                    -> {}
            }
        }
        refreshData()
        dispatchTxUpdated()
    }

    fun isCanceled(): Boolean {
        refreshData()
        return this.inner.isCancelled
    }

    fun isDone(): Boolean {
        refreshData()
        return this.inner.isDone
    }

    fun timedOut(): Boolean {
        refreshData()
        return this.inner.timedOut
    }

    fun canBePresented(): Boolean {
        refreshData()
        return this.inner.canBePresented
    }

    fun accept(): OutgoingVerificationRequest? {
        return this.machine.acceptSasVerification(this.inner.otherUserId, inner.flowId)
    }

    @Throws(CryptoStoreErrorException::class)
    suspend fun confirm(): OutgoingVerificationRequest? =
            withContext(Dispatchers.IO) {
                machine.confirmVerification(inner.otherUserId, inner.flowId)
            }

    fun cancelHelper(): OutgoingVerificationRequest? {
        return this.machine.cancelVerification(this.inner.otherUserId, inner.flowId)
    }

    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        val emojiIndex = this.machine.getEmojiIndex(this.inner.otherUserId, this.inner.flowId)

        return emojiIndex?.map { getEmojiForCode(it) } ?: listOf()
    }

    override fun getDecimalCodeRepresentation(): String {
        val decimals = this.machine.getDecimals(this.inner.otherUserId, this.inner.flowId)

        return decimals?.joinToString(" ") ?: ""
    }
}
