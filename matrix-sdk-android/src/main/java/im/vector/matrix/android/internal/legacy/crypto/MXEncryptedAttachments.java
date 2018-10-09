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

package im.vector.matrix.android.internal.legacy.crypto;

import android.text.TextUtils;
import android.util.Base64;

import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileKey;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MXEncryptedAttachments implements Serializable {
    private static final String LOG_TAG = MXEncryptedAttachments.class.getSimpleName();

    private static final int CRYPTO_BUFFER_SIZE = 32 * 1024;
    private static final String CIPHER_ALGORITHM = "AES/CTR/NoPadding";
    private static final String SECRET_KEY_SPEC_ALGORITHM = "AES";
    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";

    /**
     * Define the result of an encryption file
     */
    public static class EncryptionResult {
        public EncryptedFileInfo mEncryptedFileInfo;
        public InputStream mEncryptedStream;

        public EncryptionResult() {
        }
    }

    /***
     * Encrypt an attachment stream.
     * @param attachmentStream the attachment stream
     * @param mimetype the mime type
     * @return the encryption file info
     */
    public static EncryptionResult encryptAttachment(InputStream attachmentStream, String mimetype) {
        long t0 = System.currentTimeMillis();
        SecureRandom secureRandom = new SecureRandom();

        // generate a random iv key
        // Half of the IV is random, the lower order bits are zeroed
        // such that the counter never wraps.
        // See https://github.com/matrix-org/matrix-ios-kit/blob/3dc0d8e46b4deb6669ed44f72ad79be56471354c/MatrixKit/Models/Room/MXEncryptedAttachments.m#L75
        byte[] initVectorBytes = new byte[16];
        Arrays.fill(initVectorBytes, (byte) 0);

        byte[] ivRandomPart = new byte[8];
        secureRandom.nextBytes(ivRandomPart);

        System.arraycopy(ivRandomPart, 0, initVectorBytes, 0, ivRandomPart.length);

        byte[] key = new byte[32];
        secureRandom.nextBytes(key);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            Cipher encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initVectorBytes);
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            MessageDigest messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);

            byte[] data = new byte[CRYPTO_BUFFER_SIZE];
            int read;
            byte[] encodedBytes;

            while (-1 != (read = attachmentStream.read(data))) {
                encodedBytes = encryptCipher.update(data, 0, read);
                messageDigest.update(encodedBytes, 0, encodedBytes.length);
                outStream.write(encodedBytes);
            }

            // encrypt the latest chunk
            encodedBytes = encryptCipher.doFinal();
            messageDigest.update(encodedBytes, 0, encodedBytes.length);
            outStream.write(encodedBytes);

            EncryptionResult result = new EncryptionResult();
            result.mEncryptedFileInfo = new EncryptedFileInfo();
            result.mEncryptedFileInfo.key = new EncryptedFileKey();
            result.mEncryptedFileInfo.mimetype = mimetype;
            result.mEncryptedFileInfo.key.alg = "A256CTR";
            result.mEncryptedFileInfo.key.ext = true;
            result.mEncryptedFileInfo.key.key_ops = Arrays.asList("encrypt", "decrypt");
            result.mEncryptedFileInfo.key.kty = "oct";
            result.mEncryptedFileInfo.key.k = base64ToBase64Url(Base64.encodeToString(key, Base64.DEFAULT));
            result.mEncryptedFileInfo.iv = Base64.encodeToString(initVectorBytes, Base64.DEFAULT).replace("\n", "").replace("=", "");
            result.mEncryptedFileInfo.v = "v2";

            result.mEncryptedFileInfo.hashes = new HashMap<>();
            result.mEncryptedFileInfo.hashes.put("sha256", base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT)));

            result.mEncryptedStream = new ByteArrayInputStream(outStream.toByteArray());
            outStream.close();

            Log.d(LOG_TAG, "Encrypt in " + (System.currentTimeMillis() - t0) + " ms");
            return result;
        } catch (OutOfMemoryError oom) {
            Log.e(LOG_TAG, "## encryptAttachment failed " + oom.getMessage(), oom);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## encryptAttachment failed " + e.getMessage(), e);
        }

        try {
            outStream.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## encryptAttachment() : fail to close outStream", e);
        }

        return null;
    }

    /**
     * Decrypt an attachment
     *
     * @param attachmentStream  the attachment stream
     * @param encryptedFileInfo the encryption file info
     * @return the decrypted attachment stream
     */
    public static InputStream decryptAttachment(InputStream attachmentStream, EncryptedFileInfo encryptedFileInfo) {
        // sanity checks
        if ((null == attachmentStream) || (null == encryptedFileInfo)) {
            Log.e(LOG_TAG, "## decryptAttachment() : null parameters");
            return null;
        }

        if (TextUtils.isEmpty(encryptedFileInfo.iv)
                || (null == encryptedFileInfo.key)
                || (null == encryptedFileInfo.hashes)
                || !encryptedFileInfo.hashes.containsKey("sha256")) {
            Log.e(LOG_TAG, "## decryptAttachment() : some fields are not defined");
            return null;
        }

        if (!TextUtils.equals(encryptedFileInfo.key.alg, "A256CTR")
                || !TextUtils.equals(encryptedFileInfo.key.kty, "oct")
                || TextUtils.isEmpty(encryptedFileInfo.key.k)) {
            Log.e(LOG_TAG, "## decryptAttachment() : invalid key fields");
            return null;
        }

        // detect if there is no data to decrypt
        try {
            if (0 == attachmentStream.available()) {
                return new ByteArrayInputStream(new byte[0]);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Fail to retrieve the file size", e);
        }

        long t0 = System.currentTimeMillis();

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            byte[] key = Base64.decode(base64UrlToBase64(encryptedFileInfo.key.k), Base64.DEFAULT);
            byte[] initVectorBytes = Base64.decode(encryptedFileInfo.iv, Base64.DEFAULT);

            Cipher decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initVectorBytes);
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            MessageDigest messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);

            int read;
            byte[] data = new byte[CRYPTO_BUFFER_SIZE];
            byte[] decodedBytes;

            while (-1 != (read = attachmentStream.read(data))) {
                messageDigest.update(data, 0, read);
                decodedBytes = decryptCipher.update(data, 0, read);
                outStream.write(decodedBytes);
            }

            // decrypt the last chunk
            decodedBytes = decryptCipher.doFinal();
            outStream.write(decodedBytes);

            String currentDigestValue = base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT));

            if (!TextUtils.equals(encryptedFileInfo.hashes.get("sha256"), currentDigestValue)) {
                Log.e(LOG_TAG, "## decryptAttachment() :  Digest value mismatch");
                outStream.close();
                return null;
            }

            InputStream decryptedStream = new ByteArrayInputStream(outStream.toByteArray());
            outStream.close();

            Log.d(LOG_TAG, "Decrypt in " + (System.currentTimeMillis() - t0) + " ms");

            return decryptedStream;
        } catch (OutOfMemoryError oom) {
            Log.e(LOG_TAG, "## decryptAttachment() :  failed " + oom.getMessage(), oom);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## decryptAttachment() :  failed " + e.getMessage(), e);
        }

        try {
            outStream.close();
        } catch (Exception closeException) {
            Log.e(LOG_TAG, "## decryptAttachment() :  fail to close the file", closeException);
        }

        return null;
    }

    /**
     * Base64 URL conversion methods
     */

    private static String base64UrlToBase64(String base64Url) {
        if (null != base64Url) {
            base64Url = base64Url.replaceAll("-", "+");
            base64Url = base64Url.replaceAll("_", "/");
        }

        return base64Url;
    }

    private static String base64ToBase64Url(String base64) {
        if (null != base64) {
            base64 = base64.replaceAll("\n", "");
            base64 = base64.replaceAll("\\+", "-");
            base64 = base64.replaceAll("/", "_");
            base64 = base64.replaceAll("=", "");
        }
        return base64;
    }

    private static String base64ToUnpaddedBase64(String base64) {
        if (null != base64) {
            base64 = base64.replaceAll("\n", "");
            base64 = base64.replaceAll("=", "");
        }

        return base64;
    }
}
