/*
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

package im.vector.matrix.android.internal.legacy.util;

import javax.crypto.SecretKey;

/**
 * Tuple which contains the secret key and the version of Android when the key has been generated
 */
public class SecretKeyAndVersion {
    // The key
    private final SecretKey secretKey;

    // the android version when the key has been generated
    private final int androidVersionWhenTheKeyHasBeenGenerated;

    /**
     * @param secretKey                       the key
     * @param androidVersionWhenTheKeyHasBeenGenerated the android version when the key has been generated
     */
    public SecretKeyAndVersion(SecretKey secretKey, int androidVersionWhenTheKeyHasBeenGenerated) {
        this.secretKey = secretKey;
        this.androidVersionWhenTheKeyHasBeenGenerated = androidVersionWhenTheKeyHasBeenGenerated;
    }

    /**
     * Get the key
     *
     * @return the key
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * Get the android version when the key has been generated
     *
     * @return the android version when the key has been generated
     */
    public int getAndroidVersionWhenTheKeyHasBeenGenerated() {
        return androidVersionWhenTheKeyHasBeenGenerated;
    }
}
