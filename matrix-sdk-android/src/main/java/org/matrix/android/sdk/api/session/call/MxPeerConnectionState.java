/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call;

/**
 * This is a copy of https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/connectionState
 * to avoid having the dependency over WebRtc library on sdk.
 */
public enum MxPeerConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED
}
