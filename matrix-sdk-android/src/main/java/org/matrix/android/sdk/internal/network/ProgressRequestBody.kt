/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import org.matrix.android.sdk.api.extensions.tryOrNull
import java.io.IOException

internal class ProgressRequestBody(private val delegate: RequestBody,
                                   private val listener: Listener) : RequestBody() {

    private lateinit var countingSink: CountingSink

    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun isOneShot() = delegate.isOneShot()

    override fun isDuplex() = delegate.isDuplex()

    val length = tryOrNull { delegate.contentLength() } ?: -1

    override fun contentLength() = length

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

        private var bytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        fun onProgress(current: Long, total: Long)
    }
}
