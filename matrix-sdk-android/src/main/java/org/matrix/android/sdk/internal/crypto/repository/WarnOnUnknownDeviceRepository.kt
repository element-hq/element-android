/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.repository

import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class WarnOnUnknownDeviceRepository @Inject constructor() {

    // TODO: set it back to true by default. Need UI
    // Warn the user if some new devices are detected while encrypting a message.
    private var warnOnUnknownDevices = false

    /**
     * Tells if the encryption must fail if some unknown devices are detected.
     *
     * @return true to warn when some unknown devices are detected.
     */
    fun warnOnUnknownDevices() = warnOnUnknownDevices

    /**
     * Update the warn status when some unknown devices are detected.
     *
     * @param warn true to warn when some unknown devices are detected.
     */
    fun setWarnOnUnknownDevices(warn: Boolean) {
        warnOnUnknownDevices = warn
    }
}
