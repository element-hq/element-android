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
package im.vector.riotx.features.crypto.verification

import android.content.Context
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.riotx.R
import im.vector.riotx.features.popup.PopupAlertManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the VerificationManager and add a new notification when an incoming request is detected.
 */
@Singleton
class IncomingVerificationRequestHandler @Inject constructor(private val context: Context) : SasVerificationService.SasVerificationListener {

    private var session: Session? = null

    fun start(session: Session) {
        this.session = session
        session.getSasVerificationService().addListener(this)
    }

    fun stop() {
        session?.getSasVerificationService()?.removeListener(this)
        this.session = null
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {}

    override fun transactionUpdated(tx: SasVerificationTransaction) {
        when (tx.state) {
            SasVerificationTxState.OnStarted -> {
                // Add a notification for every incoming request
                val name = session?.getUser(tx.otherUserId)?.displayName
                        ?: tx.otherUserId

                val alert = PopupAlertManager.VectorAlert(
                        "kvr_${tx.transactionId}",
                        context.getString(R.string.sas_incoming_request_notif_title),
                        context.getString(R.string.sas_incoming_request_notif_content, name),
                        R.drawable.shield)
                        .apply {
                            contentAction = Runnable {
                                val intent = SASVerificationActivity.incomingIntent(context,
                                        session?.myUserId  ?: "",
                                        tx.otherUserId,
                                        tx.transactionId)
                                weakCurrentActivity?.get()?.startActivity(intent)
                            }
                            dismissedAction = Runnable {
                                tx.cancel()
                            }
                            addButton(
                                    context.getString(R.string.ignore),
                                    Runnable {
                                        tx.cancel()
                                    }
                            )
                            addButton(
                                    context.getString(R.string.action_open),
                                    Runnable {
                                        val intent = SASVerificationActivity.incomingIntent(context,
                                                session?.myUserId ?: "",
                                                tx.otherUserId,
                                                tx.transactionId)
                                        weakCurrentActivity?.get()?.startActivity(intent)
                                    }
                            )
                            // 10mn expiration
                            expirationTimestamp = System.currentTimeMillis() + (10 * 60 * 1000L)
                        }
                PopupAlertManager.postVectorAlert(alert)
            }
            SasVerificationTxState.Cancelled,
            SasVerificationTxState.OnCancelled,
            SasVerificationTxState.Verified  -> {
                // cancel related notification
                PopupAlertManager.cancelAlert("kvr_${tx.transactionId}")
            }
            else                             -> Unit
        }
    }

    override fun markedAsManuallyVerified(userId: String, deviceId: String) {
    }
}
