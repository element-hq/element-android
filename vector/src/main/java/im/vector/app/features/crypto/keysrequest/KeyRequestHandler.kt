/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.keysrequest

import android.content.Context
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.features.popup.DefaultVectorAlert
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.session.coroutineScope
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.SecretShareRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manage the key share events.
 * Listens for incoming key request and display an alert to the user asking him to ignore / verify
 * calling device / or accept without verifying.
 * If several requests come from same user/device, a single alert is displayed (this alert will accept/reject all request
 * depending on user action)
 */

// TODO Do we ever request to users anymore?
@Singleton
class KeyRequestHandler @Inject constructor(
        private val context: Context,
        private val popupAlertManager: PopupAlertManager,
        private val dateFormatter: VectorDateFormatter
) : GossipingRequestListener,
        VerificationService.Listener {

    private val alertsToRequests = HashMap<String, ArrayList<IncomingRoomKeyRequest>>()

    var session: Session? = null

    var scope: CoroutineScope? = null

    // This functionality is disabled in element for now. As it could be prone to social attacks
    var enablePromptingForRequest = false

    //    lateinit var listenerJob: Job
    fun start(session: Session) {
        this.session = session
        val scope = CoroutineScope(SupervisorJob() + session.coroutineScope.coroutineContext)
        this.scope = scope
        session.cryptoService().verificationService().requestEventFlow()
                .cancellable()
                .onEach {
                    when (it) {
                        is VerificationEvent.RequestAdded -> verificationRequestCreated(it.request)
                        is VerificationEvent.RequestUpdated -> verificationRequestUpdated(it.request)
                        is VerificationEvent.TransactionAdded -> transactionCreated(it.transaction)
                        is VerificationEvent.TransactionUpdated -> transactionUpdated(it.transaction)
                    }
                }.launchIn(scope)

        session.cryptoService().addRoomKeysRequestListener(this)
    }

    fun stop() {
        scope?.cancel()
        scope = null
        // session?.cryptoService()?.verificationService()?.removeListener(this)
        session?.cryptoService()?.removeRoomKeysRequestListener(this)
        session = null
    }

    override fun onSecretShareRequest(request: SecretShareRequest): Boolean {
        // By default Element will not prompt if the SDK has decided that the request should not be fulfilled
        Timber.v("## onSecretShareRequest() : Ignoring $request")
        return true
    }

    /**
     * Handle incoming key request.
     *
     * @param request the key request.
     */
    override fun onRoomKeyRequest(request: IncomingRoomKeyRequest) {
        if (!enablePromptingForRequest) return

        val userId = request.userId
        val deviceId = request.deviceId
        val requestId = request.requestId

        if (userId.isNullOrBlank() || deviceId.isNullOrBlank() || requestId.isNullOrBlank()) {
            Timber.e("## handleKeyRequest() : invalid parameters")
            return
        }

        // Do we already have alerts for this user/device
        val mappingKey = keyForMap(userId, deviceId)
        if (alertsToRequests.containsKey(mappingKey)) {
            // just add the request, there is already an alert for this
            alertsToRequests[mappingKey]?.add(request)
            return
        }

        alertsToRequests[mappingKey] = ArrayList<IncomingRoomKeyRequest>().apply { this.add(request) }

        scope?.launch {
            try {
                val data = session?.cryptoService()?.downloadKeysIfNeeded(listOf(userId), false)
                        ?: return@launch
                val deviceInfo = data.getObject(userId, deviceId)

                if (null == deviceInfo) {
                    Timber.e("## displayKeyShareDialog() : No details found for device $userId:$deviceId")
                    // ignore
                    return@launch
                }

                if (deviceInfo.isUnknown) {
                    session?.cryptoService()?.verificationService()?.markedLocallyAsManuallyVerified(userId, deviceId)

                    deviceInfo.trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)

                    // can we get more info on this device?
                    session?.cryptoService()?.getMyDevicesInfo()?.firstOrNull { it.deviceId == deviceId }?.let {
                        withContext(Dispatchers.Main) {
                            postAlert(context, userId, deviceId, true, deviceInfo, it)
                        }
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            postAlert(context, userId, deviceId, true, deviceInfo)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        postAlert(context, userId, deviceId, false, deviceInfo)
                    }
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "## displayKeyShareDialog : downloadKeys")
            }
        }
    }

    private fun postAlert(
            context: Context,
            userId: String,
            deviceId: String,
            wasNewDevice: Boolean,
            deviceInfo: CryptoDeviceInfo?,
            moreInfo: DeviceInfo? = null
    ) {
        val deviceName = if (deviceInfo!!.displayName().isNullOrEmpty()) deviceInfo.deviceId else deviceInfo.displayName()
        val dialogText: String?

        if (moreInfo != null) {
            val lastSeenIp = if (moreInfo.lastSeenIp.isNullOrBlank()) {
                context.getString(CommonStrings.encryption_information_unknown_ip)
            } else {
                moreInfo.lastSeenIp
            }

            val lastSeenTime = dateFormatter.format(moreInfo.lastSeenTs, DateFormatKind.DEFAULT_DATE_AND_TIME)
            val lastSeenInfo = context.getString(CommonStrings.devices_details_last_seen_format, lastSeenIp, lastSeenTime)
            dialogText = if (wasNewDevice) {
                context.getString(CommonStrings.you_added_a_new_device_with_info, deviceName, lastSeenInfo)
            } else {
                context.getString(CommonStrings.your_unverified_device_requesting_with_info, deviceName, lastSeenInfo)
            }
        } else {
            dialogText = if (wasNewDevice) {
                context.getString(CommonStrings.you_added_a_new_device, deviceName)
            } else {
                context.getString(CommonStrings.your_unverified_device_requesting, deviceName)
            }
        }

        val alert = DefaultVectorAlert(
                alertManagerId(userId, deviceId),
                context.getString(CommonStrings.key_share_request),
                dialogText,
                R.drawable.key_small
        )

        alert.colorRes = im.vector.lib.ui.styles.R.color.key_share_req_accent_color

        val mappingKey = keyForMap(userId, deviceId)
        alert.dismissedAction = Runnable {
            denyAllRequests(mappingKey)
        }

        alert.addButton(context.getString(CommonStrings.share_without_verifying_short_label), {
            shareAllSessions(mappingKey)
        })

        alert.addButton(context.getString(CommonStrings.ignore_request_short_label), {
            denyAllRequests(mappingKey)
        })

        popupAlertManager.postVectorAlert(alert)
    }

    private fun denyAllRequests(mappingKey: String) {
        alertsToRequests.remove(mappingKey)
    }

    private fun shareAllSessions(mappingKey: String) {
        alertsToRequests[mappingKey]?.forEach {
            session?.coroutineScope?.launch {
                session?.cryptoService()?.manuallyAcceptRoomKeyRequest(it)
            }
        }
        alertsToRequests.remove(mappingKey)
    }

    /**
     * Manage a cancellation request.
     *
     * @param request the cancellation request.
     */
    override fun onRequestCancelled(request: IncomingRoomKeyRequest) {
        // see if we can find the request in the queue
        val userId = request.userId
        val deviceId = request.deviceId
        val requestId = request.requestId

        if (userId.isNullOrEmpty() || deviceId.isNullOrEmpty() || requestId.isNullOrEmpty()) {
            Timber.e("## handleKeyRequestCancellation() : invalid parameters")
            return
        }

        val alertMgrUniqueKey = alertManagerId(userId, deviceId)
        alertsToRequests[alertMgrUniqueKey]?.removeAll {
            it.deviceId == request.deviceId &&
                    it.userId == request.userId &&
                    it.requestId == request.requestId
        }
        if (alertsToRequests[alertMgrUniqueKey]?.isEmpty() == true) {
            popupAlertManager.cancelAlert(alertMgrUniqueKey)
            alertsToRequests.remove(keyForMap(userId, deviceId))
        }
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx is SasVerificationTransaction) {
            val state = tx.state()
            if (state is SasTransactionState.Done) {
                // ok it's verified, see if we have key request for that
                shareAllSessions("${tx.otherDeviceId}${tx.otherUserId}")
                popupAlertManager.cancelAlert("ikr_${tx.otherDeviceId}${tx.otherUserId}")
            }
        }
        // should do it with QR tx also
        // TODO -> Probably better to listen to device trust changes?
    }

    override fun markedAsManuallyVerified(userId: String, deviceId: String) {
        // accept related requests
        shareAllSessions(keyForMap(userId, deviceId))
        popupAlertManager.cancelAlert(alertManagerId(userId, deviceId))
    }

    private fun keyForMap(userId: String, deviceId: String) = "$deviceId$userId"

    private fun alertManagerId(userId: String, deviceId: String) = "ikr_$deviceId$userId"
}
