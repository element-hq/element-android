/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.androidsdk.crypto.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */
public class MXDeviceInfo implements Serializable {
    private static final long serialVersionUID = 20129670646382964L;

    // This device is a new device and the user was not warned it has been added.
    public static final int DEVICE_VERIFICATION_UNKNOWN = -1;

    // The user has not yet verified this device.
    public static final int DEVICE_VERIFICATION_UNVERIFIED = 0;

    // The user has verified this device.
    public static final int DEVICE_VERIFICATION_VERIFIED = 1;

    // The user has blocked this device.
    public static final int DEVICE_VERIFICATION_BLOCKED = 2;

    /**
     * The id of this device.
     */
    public String deviceId;

    /**
     * the user id
     */
    public String userId;

    /**
     * The list of algorithms supported by this device.
     */
    public List<String> algorithms;

    /**
     * A map from <key type>:<id> to <base64-encoded key>>.
     */
    public Map<String, String> keys;

    /**
     * The signature of this MXDeviceInfo.
     * A map from <key type>:<device_id> to <base64-encoded key>>.
     */
    public Map<String, Map<String, String>> signatures;

    /*
     * Additional data from the homeserver.
     */
    public Map<String, Object> unsigned;

    /**
     * Verification state of this device.
     */
    public int mVerified;

    /**
     * Constructor
     */
    public MXDeviceInfo() {
        mVerified = DEVICE_VERIFICATION_UNKNOWN;
    }
}
