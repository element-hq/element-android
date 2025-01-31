/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.androidsdk.crypto.data;

import org.matrix.olm.OlmInboundGroupSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 *
 * This class adds more context to a OLMInboundGroupSession object.
 * This allows additional checks. The class implements NSCoding so that the context can be stored.
 */
public class MXOlmInboundGroupSession2 implements Serializable {
    // define a serialVersionUID to avoid having to redefine the class after updates
    private static final long serialVersionUID = 201702011617L;

    // The associated olm inbound group session.
    public OlmInboundGroupSession mSession;

    // The room in which this session is used.
    public String mRoomId;

    // The base64-encoded curve25519 key of the sender.
    public String mSenderKey;

    // Other keys the sender claims.
    public Map<String, String> mKeysClaimed;

    // Devices which forwarded this session to us (normally empty).
    public List<String> mForwardingCurve25519KeyChain = new ArrayList<>();
}