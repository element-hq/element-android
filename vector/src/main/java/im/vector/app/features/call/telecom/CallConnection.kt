/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.telecom

import android.content.Context
import android.os.Build
import android.telecom.Connection
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import im.vector.app.features.call.webrtc.WebRtcCallManager
import timber.log.Timber
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M) class CallConnection(
        private val context: Context,
        private val roomId: String,
        val callId: String
) : Connection() {

    @Inject lateinit var callManager: WebRtcCallManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectionProperties = PROPERTY_SELF_MANAGED
        }
    }

    /**
     * The telecom subsystem calls this method when you add a new incoming call and your app should show its incoming call UI.
     */
    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
        Timber.i("onShowIncomingCallUi")
        /*
        VectorCallActivity.newIntent(context, roomId).let {
            context.startActivity(it)
        }
         */
    }

    override fun onAnswer() {
        super.onAnswer()
        // startCall()
        Timber.i("onShowIncomingCallUi")
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        Timber.i("onStateChanged${stateToString(state)}")
    }

    override fun onReject() {
        super.onReject()
        Timber.i("onReject")
        close()
    }

    override fun onDisconnect() {
        onDisconnect()
        Timber.i("onDisconnect")
        close()
    }

    private fun close() {
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    private fun startCall() {
        /*
        //peerConnectionManager.createPeerConnectionFactory()
        peerConnectionManager.listener = this

        val cameraIterator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(false)
        val frontCamera = cameraIterator.deviceNames
                ?.firstOrNull { cameraIterator.isFrontFacing(it) }
                ?: cameraIterator.deviceNames?.first()
                ?: return
        val videoCapturer = cameraIterator.createCapturer(frontCamera, null)

        val iceServers = ArrayList<PeerConnection.IceServer>().apply {
            listOf("turn:turn.matrix.org:3478?transport=udp", "turn:turn.matrix.org:3478?transport=tcp", "turns:turn.matrix.org:443?transport=tcp").forEach {
                add(
                        PeerConnection.IceServer.builder(it)
                                .setUsername("xxxxx")
                                .setPassword("xxxxx")
                                .createIceServer()
                )
            }
        }

        peerConnectionManager.createPeerConnection(videoCapturer, iceServers)
        //peerConnectionManager.startCall()
         */
    }
}
