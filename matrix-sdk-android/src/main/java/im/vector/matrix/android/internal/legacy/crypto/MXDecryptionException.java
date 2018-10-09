/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.legacy.crypto;

/**
 * This class represents a decryption exception
 */
public class MXDecryptionException extends Exception {

    /**
     * Describe the decryption error.
     */
    private MXCryptoError mCryptoError;

    /**
     * Constructor
     *
     * @param cryptoError the linked crypto error
     */
    public MXDecryptionException(MXCryptoError cryptoError) {
        mCryptoError = cryptoError;
    }

    /**
     * @return the linked crypto error
     */
    public MXCryptoError getCryptoError() {
        return mCryptoError;
    }

    @Override
    public String getMessage() {
        if (null != mCryptoError) {
            return mCryptoError.getMessage();
        }

        return super.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        if (null != mCryptoError) {
            return mCryptoError.getLocalizedMessage();
        }
        return super.getLocalizedMessage();
    }
}
