/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.features.session.coroutineScope
import im.vector.lib.core.utils.compat.getParcelableCompat
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
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
        private val coroutineScope: CoroutineScope,
        private val clock: Clock,
) : VerificationService.Listener {

    private var session: Session? = null
    var scope: CoroutineScope? = null

    fun start(session: Session) {
        this.session = session
        this.scope = CoroutineScope(SupervisorJob() + session.coroutineScope.coroutineContext)
        session.cryptoService().verificationService().requestEventFlow()
                .cancellable()
                .onEach {
                    when (it) {
                        is VerificationEvent.RequestAdded -> verificationRequestCreated(it.request)
                        is VerificationEvent.RequestUpdated -> verificationRequestUpdated(it.request)
                        is VerificationEvent.TransactionAdded -> transactionCreated(it.transaction)
                        is VerificationEvent.TransactionUpdated -> transactionUpdated(it.transaction)
                    }
                }.launchIn(this.scope!!)
    }

    fun stop() {
//        session?.cryptoService()?.verificationService()?.removeListener(this)
        scope?.cancel()
        this.session = null
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (!tx.isToDeviceTransport()) return
        // TODO maybe check also if
        val uid = "kvr_${tx.transactionId}"
        // TODO we don't have that anymore? as it has to be requested first?
        if (tx !is SasVerificationTransaction) return
        when (tx.state()) {
            is SasTransactionState.SasStarted -> {
                // Add a notification for every incoming request
//                val user = session.getUserOrDefault(tx.otherUserId).toMatrixItem()
//                val name = user.getBestName()
//                val alert = VerificationVectorAlert(
//                        uid,
//                        context.getString(CommonStrings.sas_incoming_request_notif_title),
//                        context.getString(CommonStrings.sas_incoming_request_notif_content, name),
//                        R.drawable.ic_shield_black,
//                        shouldBeDisplayedIn = { activity ->
//                            if (activity is VectorBaseActivity<*>) {
//                                // TODO a bit too ugly :/
//                                activity.supportFragmentManager.findFragmentByTag(VerificationBottomSheet.WAITING_SELF_VERIF_TAG)?.let {
//                                    false.also {
//                                        popupAlertManager.cancelAlert(uid)
//                                    }
//                                } ?: true
//                            } else true
//                        }
//                )
//                        .apply {
//                            viewBinder = VerificationVectorAlert.ViewBinder(user, avatarRenderer.get())
//                            contentAction = Runnable {
//                                (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
//                                    it.navigator.performDeviceVerification(it, tx.otherUserId, tx.transactionId)
//                                }
//                            }
//                            dismissedAction = LaunchCoroutineRunnable(coroutineScope) {
//                                tx.cancel()
//                            }
//                            addButton(
//                                    context.getString(CommonStrings.action_ignore),
//                                    LaunchCoroutineRunnable(coroutineScope) {
//                                        tx.cancel()
//                                    }
//                            )
//                            addButton(
//                                    context.getString(CommonStrings.action_open),
//                                    {
//                                        (weakCurrentActivity?.get() as? VectorBaseActivity<*>)?.let {
//                                            it.navigator.performDeviceVerification(it, tx.otherUserId, tx.transactionId)
//                                        }
//                                    }
//                            )
//                            // 10mn expiration
//                            expirationTimestamp = clock.epochMillis() + (10 * 60 * 1000L)
//                        }
//                popupAlertManager.postVectorAlert(alert)
            }
            is SasTransactionState.Done -> {
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
                    title = context.getString(CommonStrings.sas_incoming_request_notif_title),
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
                                    if (pr.otherUserId == session?.myUserId) {
                                        it.navigator.showIncomingSelfVerification(it, pr.transactionId)
                                    }
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
                        dismissedAction = LaunchCoroutineRunnable(coroutineScope) {
                            session?.cryptoService()?.verificationService()?.cancelVerificationRequest(
                                    pr.otherUserId,
                                    pr.transactionId,
                            )
                        }
                        colorAttribute = im.vector.lib.ui.styles.R.attr.vctr_notice_secondary
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
        if (pr.isIncoming && (pr.state == EVerificationState.HandledByOtherSession ||
                        pr.state == EVerificationState.Cancelled ||
                        pr.state == EVerificationState.Started ||
                        pr.state == EVerificationState.WeStarted)) {
            popupAlertManager.cancelAlert(uniqueIdForVerificationRequest(pr))
        }
    }

    private class LaunchCoroutineRunnable(private val coroutineScope: CoroutineScope, private val block: suspend () -> Unit) : Runnable {
        override fun run() {
            coroutineScope.launch {
                block()
            }
        }
    }

    private fun uniqueIdForVerificationRequest(pr: PendingVerificationRequest) =
            "verificationRequest_${pr.transactionId}"
}
