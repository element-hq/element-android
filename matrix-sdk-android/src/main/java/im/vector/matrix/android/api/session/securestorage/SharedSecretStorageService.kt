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
import im.vector.matrix.android.api.listeners.ProgressListener

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
    fun generateKey(keyId: String,
                    keyName: String,
                    keySigner: KeySigner,
                    callback: MatrixCallback<SSSSKeyCreationInfo>)

    fun generateKeyWithPassphrase(keyId: String,
                                  keyName: String,
                                  passphrase: String,
                                  keySigner: KeySigner,
                                  progressListener: ProgressListener?,
                                  callback: MatrixCallback<SSSSKeyCreationInfo>)

    fun getKey(keyId: String): KeyInfoResult

    /**
     * A key can be marked as the "default" key by setting the user's account_data with event type m.secret_storage.default_key
     * to an object that has the ID of the key as its key property.
     * The default key will be used to encrypt all secrets that the user would expect to be available on all their clients.
     * Unless the user specifies otherwise, clients will try to use the default key to decrypt secrets.
     */
    fun getDefaultKey(): KeyInfoResult

    fun setDefaultKey(keyId: String, callback: MatrixCallback<Unit>)

    /**
     * Check whether we have a key with a given ID.
     *
     * @param keyId The ID of the key to check
     * @return Whether we have the key.
     */
    fun hasKey(keyId: String): Boolean

    /**
     * Store an encrypted secret on the server
     * Clients MUST ensure that the key is trusted before using it to encrypt secrets.
     *
     * @param name The name of the secret
     * @param secret The secret contents.
     * @param keys The list of (ID,privateKey) of the keys to use to encrypt the secret.
     */
    fun storeSecret(name: String, secretBase64: String, keys: List<String>?, callback: MatrixCallback<Unit>)

    /**
     * Use this call to determine which SSSSKeySpec to use for requesting secret
     */
    fun getAlgorithmsForSecret(name: String): List<KeyInfoResult>

    /**
     * Get an encrypted secret from the shared storage
     *
     * @param name The name of the secret
     * @param keyId The id of the key that should be used to decrypt (null for default key)
     * @param privateKey the passphrase/secret
     *
     * @return The decrypted value
     */
    @Throws

    fun getSecret(name: String, keyId: String?, secretKey: SSSSKeySpec, callback: MatrixCallback<String>)
}
