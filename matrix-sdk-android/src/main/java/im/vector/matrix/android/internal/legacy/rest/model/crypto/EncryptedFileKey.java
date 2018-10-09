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

import java.io.Serializable;
import java.util.List;

public class EncryptedFileKey implements Serializable {
    public String alg;
    public Boolean ext;
    public List<String> key_ops;
    public String kty;
    public String k;

    /**
     * Make a deep copy.
     *
     * @return the copy
     */
    public EncryptedFileKey deepCopy() {
        EncryptedFileKey encryptedFileKey = new EncryptedFileKey();

        encryptedFileKey.alg = alg;
        encryptedFileKey.ext = ext;
        encryptedFileKey.key_ops = key_ops;
        encryptedFileKey.kty = kty;
        encryptedFileKey.k = k;

        return encryptedFileKey;
    }
}



