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
