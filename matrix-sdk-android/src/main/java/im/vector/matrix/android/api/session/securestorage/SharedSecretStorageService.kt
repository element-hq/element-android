/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.securestorage

import im.vector.matrix.android.api.MatrixCallback

/**
 * Some features may require clients to store encrypted data on the server so that it can be shared securely between clients.
 * Clients may also wish to securely send such data directly to each other.
 * For example, key backups (MSC1219) can store the decryption key for the backups on the server, or cross-signing (MSC1756) can store the signing keys.
 *
 * https://github.com/matrix-org/matrix-doc/pull/1946
 *
 */

interface SharedSecretStorageService {

    /**
     * Add a key for encrypting secrets.
     *
     * @param algorithm the algorithm used by the key.
     * @param opts the options for the algorithm.  The properties used
     *     depend on the algorithm given.
     * @param keyId the ID of the key
     *
     * @return {string} the ID of the key
     */
    fun addKey(algorithm: String, opts: Map<String, Any>, keyId: String, callback: MatrixCallback<String>)

    /**
     * Check whether we have a key with a given ID.
     *
     * @param keyId The ID of the key to check
     * @return Whether we have the key.
     */
    fun hasKey(keyId: String): Boolean

    /**
     * Store an encrypted secret on the server
     *
     * @param name The name of the secret
     * @param secret The secret contents.
     * @param keys The IDs of the keys to use to encrypt the secret or null to use the default key.
     */
    fun storeSecret(name: String, secretBase64: String, keys: List<String>?, callback: MatrixCallback<Unit>)


    /**
     * Get an encrypted secret from the shared storage
     *
     * @param name The name of the secret
     * @param keyId The id of the key that should be used to decrypt
     * @param privateKey the passphrase/secret
     *
     * @return The decrypted value
     */
    fun getSecret(name: String, keyId: String, privateKey: String) : String

}
