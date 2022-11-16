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

import android.os.Handler
import android.os.Looper
import org.matrix.android.sdk.api.session.crypto.verification.IVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class VerificationListenersHolder @Inject constructor() {

    private val listeners = ArrayList<VerificationService.Listener>()

    private val uiHandler = Handler(Looper.getMainLooper())

    fun listeners(): List<VerificationService.Listener> = listeners

    fun addListener(listener: VerificationService.Listener) {
        uiHandler.post {
            if (!this.listeners.contains(listener)) {
                this.listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: VerificationService.Listener) {
        uiHandler.post { this.listeners.remove(listener) }
    }

    fun dispatchTxAdded(tx: VerificationTransaction) {
        uiHandler.post {
            this.listeners.forEach {
                try {
                    it.transactionCreated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    fun dispatchTxUpdated(tx: VerificationTransaction) {
        uiHandler.post {
            this.listeners.forEach {
                try {
                    it.transactionUpdated(tx)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    fun dispatchRequestAdded(verificationRequest: PendingVerificationRequest) {
        Timber.v("## SAS dispatchRequestAdded txId:${verificationRequest.transactionId} $verificationRequest")
        uiHandler.post {
            this.listeners.forEach {
                try {
                    it.verificationRequestCreated(verificationRequest)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }

    fun dispatchRequestUpdated(verificationRequest: PendingVerificationRequest) {
        uiHandler.post {
            listeners.forEach {
                try {
                    it.verificationRequestUpdated(verificationRequest)
                } catch (e: Throwable) {
                    Timber.e(e, "## Error while notifying listeners")
                }
            }
        }
    }
}
