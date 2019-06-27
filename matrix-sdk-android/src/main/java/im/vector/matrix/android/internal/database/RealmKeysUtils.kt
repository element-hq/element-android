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
package im.vector.matrix.android.internal.database

import android.content.Context
import android.util.Base64
import im.vector.matrix.android.api.util.SecretStoringUtils
import io.realm.RealmConfiguration
import timber.log.Timber
import java.security.SecureRandom

/**
 * On creation a random key is generated, this key is then encrypted using the system KeyStore.
 * The encrypted key is stored in shared preferences.
 * When the database is opened again, the encrypted key is taken from the shared pref,
 * then the Keystore is used to decrypt the key. The decrypted key is passed to the RealConfiguration.
 *
 * On android >=M, the KeyStore generates an AES key to encrypt/decrypt the database key,
 * and the encrypted key is stored with the initialization vector in base64 in the shared pref.
 * On android <M, the KeyStore cannot create AES keys, so a public/private key pair is generated,
 * then we generate a random secret key. The database key is encrypted with the secret key; The secret
 * key is encrypted with the public RSA key and stored with the encrypted key in the shared pref
 */
private object RealmKeysUtils {

    private const val ENCRYPTED_KEY_PREFIX = "REALM_ENCRYPTED_KEY"

    private val rng = SecureRandom()

    private fun generateKeyForRealm(): ByteArray {
        val keyForRealm = ByteArray(RealmConfiguration.KEY_LENGTH)
        rng.nextBytes(keyForRealm)
        return keyForRealm
    }

    /**
     * Check if there is already a key for this alias
     */
    fun hasKeyForDatabase(alias: String, context: Context): Boolean {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.contains("${ENCRYPTED_KEY_PREFIX}_$alias")
    }

    /**
     * Creates a new secure random key for this database.
     * The random key is then encrypted by the keystore, and the encrypted key is stored
     * in shared preferences.
     *
     * @return the generate key (can be passed to Realm Configuration)
     */
    fun createAndSaveKeyForDatabase(alias: String, context: Context): ByteArray {
        val key = generateKeyForRealm()
        val encodedKey = Base64.encodeToString(key, Base64.NO_PADDING)
        val toStore = SecretStoringUtils.securelyStoreString(encodedKey, alias, context)
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences
                .edit()
                .putString("${ENCRYPTED_KEY_PREFIX}_$alias", Base64.encodeToString(toStore!!, Base64.NO_PADDING))
                .apply()
        return key
    }

    /**
     * Retrieves the key for this database
     * throws if something goes wrong
     */
    fun extractKeyForDatabase(alias: String, context: Context): ByteArray {
        val sharedPreferences = getSharedPreferences(context)
        val encryptedB64 = sharedPreferences.getString("${ENCRYPTED_KEY_PREFIX}_$alias", null)
        val encryptedKey = Base64.decode(encryptedB64, Base64.NO_PADDING)
        val b64 = SecretStoringUtils.loadSecureSecret(encryptedKey, alias, context)
        return Base64.decode(b64!!, Base64.NO_PADDING)
    }

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences("im.vector.matrix.android.keys", Context.MODE_PRIVATE)
}


fun RealmConfiguration.Builder.configureEncryption(alias: String, context: Context): RealmConfiguration.Builder {
    if (RealmKeysUtils.hasKeyForDatabase(alias, context)) {
        Timber.i("Found key for alias:$alias")
        RealmKeysUtils.extractKeyForDatabase(alias, context).also {
            this.encryptionKey(it)
        }
    } else {
        Timber.i("Create key for DB alias:$alias")
        RealmKeysUtils.createAndSaveKeyForDatabase(alias, context).also {
            this.encryptionKey(it)
        }
    }
    return this
}