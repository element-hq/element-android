/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.attachments

import android.util.Base64
import org.matrix.android.sdk.internal.util.base64ToUnpaddedBase64
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

class MatrixDigestCheckInputStream(
        inputStream: InputStream?,
        private val expectedDigest: String
) : FilterInputStream(inputStream) {

    private val digest = MessageDigest.getInstance("SHA-256")

    @Throws(IOException::class)
    override fun read(): Int {
        val b = `in`.read()
        if (b >= 0) {
            digest.update(b.toByte())
        }

        if (b == -1) {
            ensureDigest()
        }
        return b
    }

    @Throws(IOException::class)
    override fun read(
            b: ByteArray,
            off: Int,
            len: Int): Int {
        val n = `in`.read(b, off, len)
        if (n > 0) {
            digest.update(b, off, n)
        }

        if (n == -1) {
            ensureDigest()
        }
        return n
    }

    @Throws(IOException::class)
    private fun ensureDigest() {
        val currentDigestValue = base64ToUnpaddedBase64(Base64.encodeToString(digest.digest(), Base64.DEFAULT))
        if (currentDigestValue != expectedDigest) {
            throw IOException("Bad digest")
        }
    }
}
