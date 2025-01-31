/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import javax.inject.Inject

@SessionScope
internal class SessionState @Inject constructor() {
    var isOpen = false
        private set

    /**
     * Set the new state. Throw if already in the new state.
     */
    fun setIsOpen(newState: Boolean) {
        assert(newState != isOpen)
        isOpen = newState
    }
}
