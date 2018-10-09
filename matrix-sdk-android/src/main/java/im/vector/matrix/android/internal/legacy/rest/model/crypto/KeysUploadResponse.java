/* 
 * Copyright 2016 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.crypto;

import android.text.TextUtils;

import java.util.Map;

/**
 * This class represents the response to /keys/upload request made by uploadKeys.
 */
public class KeysUploadResponse {

    /**
     * The count per algorithm as returned by the home server: a map (algorithm to count).
     */
    public Map<String, Integer> oneTimeKeyCounts;

    /**
     * Helper methods to extract information from 'oneTimeKeyCounts'
     *
     * @param algorithm the expected algorithm
     * @return the time key counts
     */
    public int oneTimeKeyCountsForAlgorithm(String algorithm) {
        int res = 0;

        if ((null != oneTimeKeyCounts) && !TextUtils.isEmpty(algorithm)) {
            Integer val = oneTimeKeyCounts.get(algorithm);

            if (null != val) {
                res = val.intValue();
            }
        }

        return res;
    }

    /**
     * Tells if there is a oneTimeKeys for a dedicated algorithm.
     *
     * @param algorithm the algorithm
     * @return true if it is found
     */
    public boolean hasOneTimeKeyCountsForAlgorithm(String algorithm) {
        return (null != oneTimeKeyCounts) && (null != algorithm) && oneTimeKeyCounts.containsKey(algorithm);
    }
}

