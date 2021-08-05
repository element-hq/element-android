/*
 * Copyright (c) 2020 New Vector Ltd
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
