/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber

open class SdpObserverAdapter : SdpObserver {
    override fun onSetFailure(p0: String?) {
        Timber.e("## SdpObserver: onSetFailure $p0")
    }

    override fun onSetSuccess() {
        Timber.v("## SdpObserver: onSetSuccess")
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
        Timber.v("## SdpObserver: onCreateSuccess $p0")
    }

    override fun onCreateFailure(p0: String?) {
        Timber.e("## SdpObserver: onCreateFailure $p0")
    }
}
