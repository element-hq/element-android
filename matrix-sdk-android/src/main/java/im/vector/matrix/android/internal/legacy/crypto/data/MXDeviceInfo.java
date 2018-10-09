/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy.crypto.data;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MXDeviceInfo implements Serializable {
    private static final long serialVersionUID = 20129670646382964L;
    //
    //private static final String LOG_TAG = "MXDeviceInfo";

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
     * Additional data from the home server.
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

    /**
     * Constructor
     *
     * @param aDeviceId the device id
     */
    public MXDeviceInfo(String aDeviceId) {
        deviceId = aDeviceId;
        mVerified = DEVICE_VERIFICATION_UNKNOWN;
    }

    /**
     * Tells if the device is unknown
     *
     * @return true if the device is unknown
     */
    public boolean isUnknown() {
        return mVerified == DEVICE_VERIFICATION_UNKNOWN;
    }

    /**
     * Tells if the device is verified.
     *
     * @return true if the device is verified
     */
    public boolean isVerified() {
        return mVerified == DEVICE_VERIFICATION_VERIFIED;
    }

    /**
     * Tells if the device is unverified.
     *
     * @return true if the device is unverified
     */
    public boolean isUnverified() {
        return mVerified == DEVICE_VERIFICATION_UNVERIFIED;
    }

    /**
     * Tells if the device is blocked.
     *
     * @return true if the device is blocked
     */
    public boolean isBlocked() {
        return mVerified == DEVICE_VERIFICATION_BLOCKED;
    }

    /**
     * @return the fingerprint
     */
    public String fingerprint() {
        if ((null != keys) && !TextUtils.isEmpty(deviceId)) {
            return keys.get("ed25519:" + deviceId);
        }

        return null;
    }

    /**
     * @return the identity key
     */
    public String identityKey() {
        if ((null != keys) && !TextUtils.isEmpty(deviceId)) {
            return keys.get("curve25519:" + deviceId);
        }

        return null;
    }

    /**
     * @return the display name
     */
    public String displayName() {
        if (null != unsigned) {
            return (String) unsigned.get("device_display_name");
        }

        return null;
    }

    /**
     * @return the signed data map
     */
    public Map<String, Object> signalableJSONDictionary() {
        Map<String, Object> map = new HashMap<>();

        map.put("device_id", deviceId);

        if (null != userId) {
            map.put("user_id", userId);
        }

        if (null != algorithms) {
            map.put("algorithms", algorithms);
        }

        if (null != keys) {
            map.put("keys", keys);
        }

        return map;
    }

    /**
     * @return a dictionary of the parameters
     */
    public Map<String, Object> JSONDictionary() {
        Map<String, Object> JSONDictionary = new HashMap<>();

        JSONDictionary.put("device_id", deviceId);

        if (null != userId) {
            JSONDictionary.put("user_id", userId);
        }

        if (null != algorithms) {
            JSONDictionary.put("algorithms", algorithms);
        }

        if (null != keys) {
            JSONDictionary.put("keys", keys);
        }

        if (null != signatures) {
            JSONDictionary.put("signatures", signatures);
        }

        if (null != unsigned) {
            JSONDictionary.put("unsigned", unsigned);
        }

        return JSONDictionary;
    }

    @Override
    public java.lang.String toString() {
        return "MXDeviceInfo " + userId + ":" + deviceId;
    }
}