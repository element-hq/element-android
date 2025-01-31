/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

/**
 * RoomEncryptionTrustLevel represents the trust level in an encrypted room.
 */
enum class RoomEncryptionTrustLevel {
    /**
     * No one in the room has been verified -> Black shield.
     */
    Default,

    /**
     * There are one or more device un-verified -> the app should display a red shield.
     */
    Warning,

    /**
     * All devices in the room are verified -> the app should display a green shield.
     */
    Trusted,

    /**
     * e2e is active but with an unsupported algorithm.
     */
    E2EWithUnsupportedAlgorithm
}
