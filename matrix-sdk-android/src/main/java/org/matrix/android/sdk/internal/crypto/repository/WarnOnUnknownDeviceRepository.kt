/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.repository

import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class WarnOnUnknownDeviceRepository @Inject constructor() {

    // TODO set it back to true by default. Need UI
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
