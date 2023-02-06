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
package im.vector.app.features.crypto.verification

import android.content.Context
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.popup.VerificationVectorAlert
import im.vector.lib.core.utils.compat.getParcelableCompat
import im.vector.lib.core.utils.timer.Clock
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Listens to the VerificationManager and add a new notification when an incoming request is detected.
 */
@Singleton
class IncomingVerificationRequestHandler @Inject constructor(
        private val context: Context,
        private var avatarRenderer: Provider<AvatarRenderer>,
        private val popupAlertManager: PopupAlertManager,
        private val clock: Clock,
) : VerificationService.Listener {

    private var session: Session? = null

    fun start(session: Session) {
        this.session = session
        session.cryptoService().verificationService().addListener(this)
    }

    fun stop() {
        session?.cryptoService()?.verificationService()?.removeListener(this)
        this.session = null
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (!tx.isToDeviceTransport()) return
        // TODO maybe check also if
        val uid = "kvr_${tx.transactionId}"
        when (tx.state) {
            is VerificationTxState.OnStarted -> {
                // Add a notification for every incoming request
                val user = session.getUserOrDefault(tx.otherUserId).toMatrixItem()
                val name = user.getBestName()
                val alert = VerificationVectorAlert(
                        uid = uid,
                        title = context.getString(R.string.sas_incoming_request_notif_title),
                        description = context.getString(R.string.sas_incoming_request_notif_content, name),
                        iconId = R.drawable.ic_shield_black,
                        priority = PopupAlertManager.INCOMING_VERIFICATION_REQUEST_PRIORITY,
                        shouldBeDisplayedIn = { activity ->
                            if (activity is VectorBaseActivity<*>) {
                                // TODO a bit too ugly :/
                                activity.supportFragmentManager.findFragmentByTag(VerificationBottomSheet.WAITING_SELF_VERIF_TAG)?.let {
                                    false.also {
                                        popupAlertManager.cancelAlert(uid)
                                    }
                                } ?: true
                            } else true
                        },
                )
                        .apply {
                            viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer.get())
                            contentAction = Runnable {
                                (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                                    it.navigator.performDeviceVerification(it, tx.otherUserId, tx.transactionId)
                                }
                            }
                            dismissedAction = Runnable {
                                tx.cancel()
                            }
                            addButton(
                                    context.getString(R.string.action_ignore),
                                    { tx.cancel() }
                            )
                            addButton(
                                    context.getString(R.string.action_open),
                                    {
                                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                                            it.navigator.performDeviceVerification(it, tx.otherUserId, tx.transactionId)
                                        }
                                    }
                            )
                            // 10mn expiration
                            expirationTimestamp = clock.epochMillis() + (10 * 60 * 1000L)
                        }
                popupAlertManager.postVectorAlert(alert)
            }
            is VerificationTxState.TerminalTxState -> {
                // cancel related notification
                popupAlertManager.cancelAlert(uid)
            }
            else -> Unit
        }
    }

    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
        Timber.v("## SAS verificationRequestCreated ${pr.transactionId}")
        // For incoming request we should prompt (if not in activity where this request apply)
        if (pr.isIncoming) {
            // if it's a self verification for my devices, we can discard the review login alert
            // if not, this request will be underneath and not visible by the user...
            // it will re-appear later
            cancelAnyVerifySessionAlerts(pr)
            val user = session.getUserOrDefault(pr.otherUserId).toMatrixItem()
            val name = user.getBestName()
            val description = if (name == pr.otherUserId) {
                name
            } else {
                "$name (${pr.otherUserId})"
            }

            val alert = VerificationVectorAlert(
                    uid = uniqueIdForVerificationRequest(pr),
                    title = context.getString(R.string.sas_incoming_request_notif_title),
                    description = description,
                    iconId = R.drawable.ic_shield_black,
                    priority = PopupAlertManager.INCOMING_VERIFICATION_REQUEST_PRIORITY,
                    shouldBeDisplayedIn = { activity ->
                        if (activity is RoomDetailActivity) {
                            activity.intent?.extras?.getParcelableCompat<TimelineArgs>(RoomDetailActivity.EXTRA_ROOM_DETAIL_ARGS)?.let {
                                it.roomId != pr.roomId
                            } ?: true
                        } else true
                    },
            )
                    .apply {
                        viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer.get())
                        contentAction = Runnable {
                            cancelAnyVerifySessionAlerts(pr)
                            (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
                                val roomId = pr.roomId
                                if (roomId.isNullOrBlank()) {
                                    it.navigator.waitSessionVerification(it)
                                } else {
                                    it.navigator.openRoom(
                                            context = it,
                                            roomId = roomId,
                                            eventId = pr.transactionId,
                                            trigger = ViewRoom.Trigger.VerificationRequest
                                    )
                                }
                            }
                        }
                        dismissedAction = Runnable {
                            session?.cryptoService()?.verificationService()?.declineVerificationRequestInDMs(
                                    pr.otherUserId,
                                    pr.transactionId ?: "",
                                    pr.roomId ?: ""
                            )
                        }
                        colorAttribute = R.attr.vctr_notice_secondary
                        // 5mn expiration
                        expirationTimestamp = clock.epochMillis() + (5 * 60 * 1000L)
                    }
            popupAlertManager.postVectorAlert(alert)
        }
    }

    private fun cancelAnyVerifySessionAlerts(pr: PendingVerificationRequest) {
        if (pr.otherUserId == session?.myUserId) {
            popupAlertManager.cancelAlert(PopupAlertManager.REVIEW_LOGIN_UID)
            popupAlertManager.cancelAlert(PopupAlertManager.VERIFY_SESSION_UID)
        }
    }

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
        // If an incoming request is readied (by another device?) we should discard the alert
        if (pr.isIncoming && (pr.isReady || pr.handledByOtherSession || pr.cancelConclusion != null)) {
            popupAlertManager.cancelAlert(uniqueIdForVerificationRequest(pr))
        }
    }

    private fun uniqueIdForVerificationRequest(pr: PendingVerificationRequest) =
            "verificationRequest_${pr.transactionId}"
}
