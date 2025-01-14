/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import org.webrtc.CameraVideoCapturer
import timber.log.Timber

open class CameraEventsHandlerAdapter : CameraVideoCapturer.CameraEventsHandler {
    override fun onCameraError(p0: String?) {
        Timber.v("## VOIP onCameraError $p0")
    }

    override fun onCameraOpening(p0: String?) {
        Timber.v("## VOIP onCameraOpening $p0")
    }

    override fun onCameraDisconnected() {
        Timber.v("## VOIP onCameraOpening")
    }

    override fun onCameraFreezed(p0: String?) {
        Timber.v("## VOIP onCameraFreezed $p0")
    }

    override fun onFirstFrameAvailable() {
        Timber.v("## VOIP onFirstFrameAvailable")
    }

    override fun onCameraClosed() {
        Timber.v("## VOIP onCameraClosed")
    }
}
