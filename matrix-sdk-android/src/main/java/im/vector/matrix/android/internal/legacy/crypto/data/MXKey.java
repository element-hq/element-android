/*
 * Copyright 2016 OpenMarket Ltd
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

import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MXKey implements Serializable {
    private static final String LOG_TAG = "MXKey";
    /**
     * Key types.
     */
    public static final String KEY_CURVE_25519_TYPE = "curve25519";
    public static final String KEY_SIGNED_CURVE_25519_TYPE = "signed_curve25519";
    //public static final String KEY_ED_25519_TYPE = "ed25519";

    /**
     * The type of the key.
     */
    public String type;

    /**
     * The id of the key.
     */
    public String keyId;

    /**
     * The key.
     */
    public String value;

    /**
     * signature user Id to [deviceid][signature]
     */
    public Map<String, Map<String, String>> signatures;

    /**
     * Default constructor
     */
    public MXKey() {
    }

    /**
     * Convert a map to a MXKey
     *
     * @param map the map to convert
     */
    public MXKey(Map<String, Map<String, Object>> map) {
        if ((null != map) && (map.size() > 0)) {
            List<String> mapKeys = new ArrayList<>(map.keySet());

            String firstEntry = mapKeys.get(0);
            setKeyFullId(firstEntry);

            Map<String, Object> params = map.get(firstEntry);
            value = (String) params.get("key");
            signatures = (Map<String, Map<String, String>>) params.get("signatures");
        }
    }

    /**
     * @return the key full id
     */
    public String getKeyFullId() {
        return type + ":" + keyId;
    }

    /**
     * Update the key fields with a key full id
     *
     * @param keyFullId the key full id
     */
    private void setKeyFullId(String keyFullId) {
        if (!TextUtils.isEmpty(keyFullId)) {
            try {
                String[] components = keyFullId.split(":");

                if (components.length == 2) {
                    type = components[0];
                    keyId = components[1];
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## setKeyFullId() failed : " + e.getMessage(), e);
            }
        }
    }

    /**
     * @return the signed data map
     */
    public Map<String, Object> signalableJSONDictionary() {
        Map<String, Object> map = new HashMap<>();

        if (null != value) {
            map.put("key", value);
        }

        return map;
    }

    /**
     * Returns a signature for an user Id and a signkey
     *
     * @param userId  the user id
     * @param signkey the sign key
     * @return the signature
     */
    public String signatureForUserId(String userId, String signkey) {
        // sanity checks
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(signkey)) {
            if ((null != signatures) && signatures.containsKey(userId)) {
                return signatures.get(userId).get(signkey);
            }
        }

        return null;
    }
}