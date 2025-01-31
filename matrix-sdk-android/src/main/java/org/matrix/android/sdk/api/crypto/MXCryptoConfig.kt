/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.crypto

/**
 * Class to define the parameters used to customize or configure the end-to-end crypto.
 */
data class MXCryptoConfig constructor(
        // Tell whether the encryption of the event content is enabled for the invited members.
        // SDK clients can disable this by settings it to false.
        // Note that the encryption for the invited members will be blocked if the history visibility is "joined".
        val enableEncryptionForInvitedMembers: Boolean = true,

        /**
         * If set to true, the SDK will automatically ignore room key request (gossiping)
         * coming from your other untrusted sessions (or blocked).
         * If set to false, the request will be forwarded to the application layer; in this
         * case the application can decide to prompt the user.
         */
        val discardRoomKeyRequestsFromUntrustedDevices: Boolean = true,

        /**
         * Currently megolm keys are requested to the sender device and to all of our devices.
         * You can limit request only to your sessions by turning this setting to `true`.
         * Forwarded keys coming from other users will also be ignored if set to true.
         */
        val limitRoomKeyRequestsToMyDevices: Boolean = true,

        )
