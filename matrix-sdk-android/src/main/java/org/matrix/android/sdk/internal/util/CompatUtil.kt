/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

@file:Suppress("DEPRECATION")

package org.matrix.android.sdk.internal.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

object CompatUtil {
    private val TAG = CompatUtil::class.java.simpleName
    private const val ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore"
    private const val AES_GCM_CIPHER_TYPE = "AES/GCM/NoPadding"
    private const val AES_GCM_KEY_SIZE_IN_BITS = 128
    private const val AES_GCM_IV_LENGTH = 12
    private const val AES_LOCAL_PROTECTION_KEY_ALIAS = "aes_local_protection"

    private const val RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS = "rsa_wrap_local_protection"
    private const val RSA_WRAP_CIPHER_TYPE = "RSA/NONE/PKCS1Padding"
    private const val AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE = "aes_wrapped_local_protection"

    private const val SHARED_KEY_ANDROID_VERSION_WHEN_KEY_HAS_BEEN_GENERATED = "android_version_when_key_has_been_generated"

    private var sSecretKeyAndVersion: SecretKeyAndVersion? = null

    /**
     * Returns the unique SecureRandom instance shared for all local storage encryption operations.
     */
    private val prng: SecureRandom by lazy(LazyThreadSafetyMode.NONE) { SecureRandom() }

    /**
     * Returns the AES key used for local storage encryption/decryption with AES/GCM.
     * The key is created if it does not exist already in the keystore.
     * From Marshmallow, this key is generated and operated directly from the android keystore.
     * From KitKat and before Marshmallow, this key is stored in the application shared preferences
     * wrapped by a RSA key generated and operated directly from the android keystore.
     *
     * @param context the context holding the application shared preferences
     */
    @Synchronized
    @Throws(KeyStoreException::class,
            CertificateException::class,
            NoSuchAlgorithmException::class,
            IOException::class,
            NoSuchProviderException::class,
            InvalidAlgorithmParameterException::class,
            NoSuchPaddingException::class,
            InvalidKeyException::class,
            IllegalBlockSizeException::class,
            UnrecoverableKeyException::class)
    private fun getAesGcmLocalProtectionKey(context: Context): SecretKeyAndVersion {
        if (sSecretKeyAndVersion == null) {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER)
            keyStore.load(null)

            Timber.i(TAG, "Loading local protection key")

            var key: SecretKey?

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            // Get the version of Android when the key has been generated, default to the current version of the system. In this case, the
            // key will be generated
            val androidVersionWhenTheKeyHasBeenGenerated = sharedPreferences
                    .getInt(SHARED_KEY_ANDROID_VERSION_WHEN_KEY_HAS_BEEN_GENERATED, Build.VERSION.SDK_INT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (keyStore.containsAlias(AES_LOCAL_PROTECTION_KEY_ALIAS)) {
                    Timber.i(TAG, "AES local protection key found in keystore")
                    key = keyStore.getKey(AES_LOCAL_PROTECTION_KEY_ALIAS, null) as SecretKey
                } else {
                    // Check if a key has been created on version < M (in case of OS upgrade)
                    key = readKeyApiL(sharedPreferences, keyStore)

                    if (key == null) {
                        Timber.i(TAG, "Generating AES key with keystore")
                        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_PROVIDER)
                        generator.init(
                                KeyGenParameterSpec.Builder(AES_LOCAL_PROTECTION_KEY_ALIAS,
                                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                        .setKeySize(AES_GCM_KEY_SIZE_IN_BITS)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                        .build())
                        key = generator.generateKey()

                        sharedPreferences.edit {
                            putInt(SHARED_KEY_ANDROID_VERSION_WHEN_KEY_HAS_BEEN_GENERATED, Build.VERSION.SDK_INT)
                        }
                    }
                }
            } else {
                key = readKeyApiL(sharedPreferences, keyStore)

                if (key == null) {
                    Timber.i(TAG, "Generating RSA key pair with keystore")
                    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE_PROVIDER)
                    val start = Calendar.getInstance()
                    val end = Calendar.getInstance()
                    end.add(Calendar.YEAR, 10)

                    generator.initialize(
                            KeyPairGeneratorSpec.Builder(context)
                                    .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                                    .setAlias(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS)
                                    .setSubject(X500Principal("CN=matrix-android-sdk"))
                                    .setStartDate(start.time)
                                    .setEndDate(end.time)
                                    .setSerialNumber(BigInteger.ONE)
                                    .build())
                    val keyPair = generator.generateKeyPair()

                    Timber.i(TAG, "Generating wrapped AES key")

                    val aesKeyRaw = ByteArray(AES_GCM_KEY_SIZE_IN_BITS / java.lang.Byte.SIZE)
                    prng.nextBytes(aesKeyRaw)
                    key = SecretKeySpec(aesKeyRaw, "AES")

                    val cipher = Cipher.getInstance(RSA_WRAP_CIPHER_TYPE)
                    cipher.init(Cipher.WRAP_MODE, keyPair.public)
                    val wrappedAesKey = cipher.wrap(key)

                    sharedPreferences.edit {
                        putString(AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE, Base64.encodeToString(wrappedAesKey, 0))
                        putInt(SHARED_KEY_ANDROID_VERSION_WHEN_KEY_HAS_BEEN_GENERATED, Build.VERSION.SDK_INT)
                    }
                }
            }

            sSecretKeyAndVersion = SecretKeyAndVersion(key!!, androidVersionWhenTheKeyHasBeenGenerated)
        }

        return sSecretKeyAndVersion!!
    }

    /**
     * Read the key, which may have been stored when the OS was < M
     *
     * @param sharedPreferences shared pref
     * @param keyStore          key store
     * @return the key if it exists or null
     */
    @Throws(KeyStoreException::class,
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            InvalidKeyException::class,
            UnrecoverableKeyException::class)
    private fun readKeyApiL(sharedPreferences: SharedPreferences, keyStore: KeyStore): SecretKey? {
        val wrappedAesKeyString = sharedPreferences.getString(AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE, null)
        if (wrappedAesKeyString != null && keyStore.containsAlias(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS)) {
            Timber.i(TAG, "RSA + wrapped AES local protection keys found in keystore")
            val privateKey = keyStore.getKey(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS, null) as PrivateKey
            val wrappedAesKey = Base64.decode(wrappedAesKeyString, 0)
            val cipher = Cipher.getInstance(RSA_WRAP_CIPHER_TYPE)
            cipher.init(Cipher.UNWRAP_MODE, privateKey)
            return cipher.unwrap(wrappedAesKey, "AES", Cipher.SECRET_KEY) as SecretKey
        }

        // Key does not exist
        return null
    }

    /**
     * Create a CipherOutputStream instance.
     * Before Kitkat, this method will return out as local storage encryption is not implemented for
     * devices before KitKat.
     *
     * @param out     the output stream
     * @param context the context holding the application shared preferences
     */
    @Throws(IOException::class,
            CertificateException::class,
            NoSuchAlgorithmException::class,
            UnrecoverableKeyException::class,
            InvalidKeyException::class,
            InvalidAlgorithmParameterException::class,
            NoSuchPaddingException::class,
            NoSuchProviderException::class,
            KeyStoreException::class,
            IllegalBlockSizeException::class)
    fun createCipherOutputStream(out: OutputStream, context: Context): OutputStream? {
        val keyAndVersion = getAesGcmLocalProtectionKey(context)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val iv: ByteArray

        if (keyAndVersion.androidVersionWhenTheKeyHasBeenGenerated >= Build.VERSION_CODES.M) {
            cipher.init(Cipher.ENCRYPT_MODE, keyAndVersion.secretKey)
            iv = cipher.iv
        } else {
            iv = ByteArray(AES_GCM_IV_LENGTH)
            prng.nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keyAndVersion.secretKey, IvParameterSpec(iv))
        }

        if (iv.size != AES_GCM_IV_LENGTH) {
            Timber.e(TAG, "Invalid IV length ${iv.size}")
            return null
        }

        out.write(iv.size)
        out.write(iv)

        return CipherOutputStream(out, cipher)
    }

    /**
     * Create a CipherInputStream instance.
     * Warning, if inputStream is not an encrypted stream, it's up to the caller to close and reopen inputStream, because the stream has been read.
     *
     * @param inputStream the input stream
     * @param context     the context holding the application shared preferences
     * @return inputStream, or the created InputStream, or null if the InputStream inputStream does not contain encrypted data
     */
    @Throws(NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            CertificateException::class,
            InvalidKeyException::class,
            KeyStoreException::class,
            UnrecoverableKeyException::class,
            IllegalBlockSizeException::class,
            NoSuchProviderException::class,
            InvalidAlgorithmParameterException::class,
            IOException::class)
    fun createCipherInputStream(inputStream: InputStream, context: Context): InputStream? {
        val ivLen = inputStream.read()
        if (ivLen != AES_GCM_IV_LENGTH) {
            Timber.e(TAG, "Invalid IV length $ivLen")
            return null
        }

        val iv = ByteArray(AES_GCM_IV_LENGTH)
        inputStream.read(iv)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)

        val keyAndVersion = getAesGcmLocalProtectionKey(context)

        val spec: AlgorithmParameterSpec = if (keyAndVersion.androidVersionWhenTheKeyHasBeenGenerated >= Build.VERSION_CODES.M) {
            GCMParameterSpec(AES_GCM_KEY_SIZE_IN_BITS, iv)
        } else {
            IvParameterSpec(iv)
        }

        cipher.init(Cipher.DECRYPT_MODE, keyAndVersion.secretKey, spec)

        return CipherInputStream(inputStream, cipher)
    }
}
