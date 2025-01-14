/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.utils

import im.vector.app.features.call.webrtc.SdpObserverAdapter
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun PeerConnection.awaitCreateOffer(mediaConstraints: MediaConstraints): SessionDescription? = suspendCoroutine { cont ->
    createOffer(object : SdpObserverAdapter() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            cont.resume(p0)
        }

        override fun onCreateFailure(p0: String?) {
            super.onCreateFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.awaitCreateAnswer(mediaConstraints: MediaConstraints): SessionDescription? = suspendCoroutine { cont ->
    createAnswer(object : SdpObserverAdapter() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            cont.resume(p0)
        }

        override fun onCreateFailure(p0: String?) {
            super.onCreateFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }
    }, mediaConstraints)
}

suspend fun PeerConnection.awaitSetLocalDescription(sessionDescription: SessionDescription): Unit = suspendCoroutine { cont ->
    setLocalDescription(object : SdpObserverAdapter() {
        override fun onSetFailure(p0: String?) {
            super.onSetFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }

        override fun onSetSuccess() {
            super.onSetSuccess()
            cont.resume(Unit)
        }
    }, sessionDescription)
}

suspend fun PeerConnection.awaitSetRemoteDescription(sessionDescription: SessionDescription): Unit = suspendCoroutine { cont ->
    setRemoteDescription(object : SdpObserverAdapter() {
        override fun onSetFailure(p0: String?) {
            super.onSetFailure(p0)
            cont.resumeWithException(IllegalStateException(p0))
        }

        override fun onSetSuccess() {
            super.onSetSuccess()
            cont.resume(Unit)
        }
    }, sessionDescription)
}
