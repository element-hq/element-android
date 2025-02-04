/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.telecom

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.StatusHints
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import im.vector.app.core.services.CallAndroidService

/**
 * No active calls in other apps
 *
 *To answer incoming calls when there are no active calls in other apps, follow these steps:
 *
 * <pre>
 *     * Your app receives a new incoming call using its usual mechanisms.
 *          - Use the addNewIncomingCall(PhoneAccountHandle, Bundle) method to inform the telecom subsystem about the new incoming call.
 *          - The telecom subsystem binds to your app's ConnectionService implementation and requests a new instance of the
 *            Connection class representing the new incoming call using the onCreateIncomingConnection(PhoneAccountHandle, ConnectionRequest) method.
 *          - The telecom subsystem informs your app that it should show its incoming call user interface using the onShowIncomingCallUi() method.
 *          - Your app shows its incoming UI using a notification with an associated full-screen intent. For more information, see onShowIncomingCallUi().
 *          - Call the setActive() method if the user accepts the incoming call, or setDisconnected(DisconnectCause) specifying REJECTED as
 *            the parameter followed by a call to the destroy() method if the user rejects the incoming call.
 *</pre>
 */
@RequiresApi(Build.VERSION_CODES.M) class VectorConnectionAndroidService : ConnectionService() {

    /**
     * The telecom subsystem calls this method in response to your app calling placeCall(Uri, Bundle) to create a new outgoing call.
     */
    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection? {
        val callId = request?.address?.encodedQuery ?: return null
        val roomId = request.extras.getString("MX_CALL_ROOM_ID") ?: return null
        return CallConnection(applicationContext, roomId, callId)
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        val roomId = request?.extras?.getString("MX_CALL_ROOM_ID") ?: return super.onCreateIncomingConnection(connectionManagerPhoneAccount, request)
        val callId = request.extras.getString("MX_CALL_CALL_ID") ?: return super.onCreateIncomingConnection(connectionManagerPhoneAccount, request)

        val connection = CallConnection(applicationContext, roomId, callId)
        connection.connectionCapabilities = Connection.CAPABILITY_MUTE
        connection.audioModeIsVoip = true
        connection.setAddress(Uri.fromParts("tel", "+905000000000", null), TelecomManager.PRESENTATION_ALLOWED)
        connection.setCallerDisplayName("Element Caller", TelecomManager.PRESENTATION_ALLOWED)
        connection.statusHints = StatusHints("Testing Hint...", null, null)

        bindService(Intent(applicationContext, CallAndroidService::class.java), CallServiceConnection(connection), 0)
        connection.setInitializing()
        return connection
    }

    inner class CallServiceConnection(private val callConnection: CallConnection) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val callSrvBinder = binder as CallAndroidService.CallServiceBinder
            callSrvBinder.getCallService().addConnection(callConnection)
            unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    companion object {
        const val TAG = "TComService"
    }
}
