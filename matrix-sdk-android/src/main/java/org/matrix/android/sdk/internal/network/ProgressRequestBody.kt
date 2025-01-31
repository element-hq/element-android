/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

internal class ProgressRequestBody(
        private val delegate: RequestBody,
        private val listener: Listener
) : RequestBody() {

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
