/*
 * Copyright 2017 OpenMarket Ltd
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

import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class to import/export the crypto data
 */
public class MXMegolmExportEncryption {
    private static final String LOG_TAG = MXMegolmExportEncryption.class.getSimpleName();

    private static final String HEADER_LINE = "-----BEGIN MEGOLM SESSION DATA-----";
    private static final String TRAILER_LINE = "-----END MEGOLM SESSION DATA-----";
    // we split into lines before base64ing, because encodeBase64 doesn't deal
    // terribly well with large arrays.
    private static final int LINE_LENGTH = (72 * 4 / 3);

    // default iteration count to export the e2e keys
    public static final int DEFAULT_ITERATION_COUNT = 500000;

    /**
     * Convert a signed byte to a int value
     *
     * @param bVal the byte value to convert
     * @return the matched int value
     */
    private static int byteToInt(byte bVal) {
        return bVal & 0xFF;
    }

    /**
     * Extract the AES key from the deriveKeys result.
     *
     * @param keyBits the deriveKeys result.
     * @return the AES key
     */
    private static byte[] getAesKey(byte[] keyBits) {
        return Arrays.copyOfRange(keyBits, 0, 32);
    }

    /**
     * Extract the Hmac key from the deriveKeys result.
     *
     * @param keyBits the deriveKeys result.
     * @return the Hmac key.
     */
    private static byte[] getHmacKey(byte[] keyBits) {
        return Arrays.copyOfRange(keyBits, 32, keyBits.length);
    }

    /**
     * Decrypt a megolm key file
     *
     * @param data     the data to decrypt
     * @param password the password.
     * @return the decrypted output.
     * @throws Exception the failure reason
     */
    public static String decryptMegolmKeyFile(byte[] data, String password) throws Exception {
        byte[] body = unpackMegolmKeyFile(data);

        // check we have a version byte
        if ((null == body) || (body.length == 0)) {
            Log.e(LOG_TAG, "## decryptMegolmKeyFile() : Invalid file: too short");
            throw new Exception("Invalid file: too short");
        }

        byte version = body[0];
        if (version != 1) {
            Log.e(LOG_TAG, "## decryptMegolmKeyFile() : Invalid file: too short");
            throw new Exception("Unsupported version");
        }

        int ciphertextLength = body.length - (1 + 16 + 16 + 4 + 32);
        if (ciphertextLength < 0) {
            throw new Exception("Invalid file: too short");
        }

        if (TextUtils.isEmpty(password)) {
            throw new Exception("Empty password is not supported");
        }

        byte[] salt = Arrays.copyOfRange(body, 1, 1 + 16);
        byte[] iv = Arrays.copyOfRange(body, 17, 17 + 16);
        int iterations = byteToInt(body[33]) << 24 | byteToInt(body[34]) << 16 | byteToInt(body[35]) << 8 | byteToInt(body[36]);
        byte[] ciphertext = Arrays.copyOfRange(body, 37, 37 + ciphertextLength);
        byte[] hmac = Arrays.copyOfRange(body, body.length - 32, body.length);

        byte[] deriveKey = deriveKeys(salt, iterations, password);

        byte[] toVerify = Arrays.copyOfRange(body, 0, body.length - 32);

        SecretKey macKey = new SecretKeySpec(getHmacKey(deriveKey), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(macKey);
        byte[] digest = mac.doFinal(toVerify);

        if (!Arrays.equals(hmac, digest)) {
            Log.e(LOG_TAG, "## decryptMegolmKeyFile() : Authentication check failed: incorrect password?");
            throw new Exception("Authentication check failed: incorrect password?");
        }

        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");

        SecretKeySpec secretKeySpec = new SecretKeySpec(getAesKey(deriveKey), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outStream.write(decryptCipher.update(ciphertext));
        outStream.write(decryptCipher.doFinal());

        String decodedString = new String(outStream.toByteArray(), "UTF-8");
        outStream.close();

        return decodedString;
    }

    /**
     * Encrypt a string into the megolm export format.
     *
     * @param data     the data to encrypt.
     * @param password the password
     * @return the encrypted data
     * @throws Exception the failure reason
     */
    public static byte[] encryptMegolmKeyFile(String data, String password) throws Exception {
        return encryptMegolmKeyFile(data, password, DEFAULT_ITERATION_COUNT);
    }

    /**
     * Encrypt a string into the megolm export format.
     *
     * @param data       the data to encrypt.
     * @param password   the password
     * @param kdf_rounds the iteration count
     * @return the encrypted data
     * @throws Exception the failure reason
     */
    public static byte[] encryptMegolmKeyFile(String data, String password, int kdf_rounds) throws Exception {
        if (TextUtils.isEmpty(password)) {
            throw new Exception("Empty password is not supported");
        }

        SecureRandom secureRandom = new SecureRandom();

        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);

        // clear bit 63 of the salt to stop us hitting the 64-bit counter boundary
        // (which would mean we wouldn't be able to decrypt on Android). The loss
        // of a single bit of salt is a price we have to pay.
        iv[9] &= 0x7f;

        byte[] deriveKey = deriveKeys(salt, kdf_rounds, password);

        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");

        SecretKeySpec secretKeySpec = new SecretKeySpec(getAesKey(deriveKey), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        decryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outStream.write(decryptCipher.update(data.getBytes("UTF-8")));
        outStream.write(decryptCipher.doFinal());

        byte[] cipherArray = outStream.toByteArray();
        int bodyLength = (1 + salt.length + iv.length + 4 + cipherArray.length + 32);

        byte[] resultBuffer = new byte[bodyLength];
        int idx = 0;
        resultBuffer[idx++] = 1; // version

        System.arraycopy(salt, 0, resultBuffer, idx, salt.length);
        idx += salt.length;

        System.arraycopy(iv, 0, resultBuffer, idx, iv.length);
        idx += iv.length;

        resultBuffer[idx++] = (byte) ((kdf_rounds >> 24) & 0xff);
        resultBuffer[idx++] = (byte) ((kdf_rounds >> 16) & 0xff);
        resultBuffer[idx++] = (byte) ((kdf_rounds >> 8) & 0xff);
        resultBuffer[idx++] = (byte) ((kdf_rounds) & 0xff);

        System.arraycopy(cipherArray, 0, resultBuffer, idx, cipherArray.length);
        idx += cipherArray.length;

        byte[] toSign = Arrays.copyOfRange(resultBuffer, 0, idx);

        SecretKey macKey = new SecretKeySpec(getHmacKey(deriveKey), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(macKey);
        byte[] digest = mac.doFinal(toSign);
        System.arraycopy(digest, 0, resultBuffer, idx, digest.length);

        return packMegolmKeyFile(resultBuffer);
    }

    /**
     * Unbase64 an ascii-armoured megolm key file
     * Strips the header and trailer lines, and unbase64s the content
     *
     * @param data the input data
     * @return unbase64ed content
     */
    private static byte[] unpackMegolmKeyFile(byte[] data) throws Exception {
        String fileStr = new String(data, "UTF-8");

        // look for the start line
        int lineStart = 0;

        while (true) {
            int lineEnd = fileStr.indexOf('\n', lineStart);

            if (lineEnd < 0) {
                Log.e(LOG_TAG, "## unpackMegolmKeyFile() : Header line not found");
                throw new Exception("Header line not found");
            }

            String line = fileStr.substring(lineStart, lineEnd).trim();

            // start the next line after the newline
            lineStart = lineEnd + 1;

            if (TextUtils.equals(line, HEADER_LINE)) {
                break;
            }
        }

        int dataStart = lineStart;

        // look for the end line
        while (true) {
            int lineEnd = fileStr.indexOf('\n', lineStart);
            String line;

            if (lineEnd < 0) {
                line = fileStr.substring(lineStart).trim();
            } else {
                line = fileStr.substring(lineStart, lineEnd).trim();
            }

            if (TextUtils.equals(line, TRAILER_LINE)) {
                break;
            }

            if (lineEnd < 0) {
                Log.e(LOG_TAG, "## unpackMegolmKeyFile() : Trailer line not found");
                throw new Exception("Trailer line not found");
            }

            // start the next line after the newline
            lineStart = lineEnd + 1;
        }

        int dataEnd = lineStart;

        // Receiving side
        return Base64.decode(fileStr.substring(dataStart, dataEnd), Base64.DEFAULT);
    }

    /**
     * Pack the megolm data.
     *
     * @param data the data to pack.
     * @return the packed data
     * @throws Exception the failure reason.
     */
    private static byte[] packMegolmKeyFile(byte[] data) throws Exception {
        int nLines = (data.length + LINE_LENGTH - 1) / LINE_LENGTH;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outStream.write(HEADER_LINE.getBytes());

        int o = 0;

        for (int i = 1; i <= nLines; i++) {
            outStream.write("\n".getBytes());

            int len = Math.min(LINE_LENGTH, data.length - o);
            outStream.write(Base64.encode(data, o, len, Base64.DEFAULT));
            o += LINE_LENGTH;
        }

        outStream.write("\n".getBytes());
        outStream.write(TRAILER_LINE.getBytes());
        outStream.write("\n".getBytes());

        return outStream.toByteArray();
    }

    /**
     * Derive the AES and HMAC-SHA-256 keys for the file
     *
     * @param salt       salt for pbkdf
     * @param iterations number of pbkdf iterations
     * @param password   password
     * @return the derived keys
     */
    private static byte[] deriveKeys(byte[] salt, int iterations, String password) throws Exception {
        Long t0 = System.currentTimeMillis();

        // based on https://en.wikipedia.org/wiki/PBKDF2 algorithm
        // it is simpler than the generic algorithm because the expected key length is equal to the mac key length.
        // noticed as dklen/hlen
        Mac prf = Mac.getInstance("HmacSHA512");
        prf.init(new SecretKeySpec(password.getBytes("UTF-8"), "HmacSHA512"));

        // 512 bits key length
        byte[] key = new byte[64];
        byte[] Uc = new byte[64];

        // U1 = PRF(Password, Salt || INT_32_BE(i))
        prf.update(salt);
        byte[] int32BE = new byte[4];
        Arrays.fill(int32BE, (byte) 0);
        int32BE[3] = (byte) 1;
        prf.update(int32BE);
        prf.doFinal(Uc, 0);

        // copy to the key
        System.arraycopy(Uc, 0, key, 0, Uc.length);

        for (int index = 2; index <= iterations; index++) {
            // Uc = PRF(Password, Uc-1)
            prf.update(Uc);
            prf.doFinal(Uc, 0);

            // F(Password, Salt, c, i) = U1 ^ U2 ^ ... ^ Uc
            for (int byteIndex = 0; byteIndex < Uc.length; byteIndex++) {
                key[byteIndex] ^= Uc[byteIndex];
            }
        }

        Log.d(LOG_TAG, "## deriveKeys() : " + iterations + " in " + (System.currentTimeMillis() - t0) + " ms");

        return key;
    }
}