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

package org.matrix.android.sdk.internal.network.ssl

import okhttp3.TlsVersion
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/**
 * Force the usage of Tls versions on every created socket
 * Inspired from https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
 */

internal class TLSSocketFactory

/**
 * Constructor
 *
 * @param trustPinned
 * @param acceptedTlsVersions
 * @throws KeyManagementException
 * @throws NoSuchAlgorithmException
 */
@Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
constructor(trustPinned: Array<TrustManager>, acceptedTlsVersions: List<TlsVersion>) : SSLSocketFactory() {

    private val internalSSLSocketFactory: SSLSocketFactory
    private val enabledProtocols: Array<String>

    init {
        val context = SSLContext.getInstance("TLS")
        context.init(null, trustPinned, SecureRandom())
        internalSSLSocketFactory = context.socketFactory
        enabledProtocols = Array(acceptedTlsVersions.size) {
            acceptedTlsVersions[it].javaName
        }
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return internalSSLSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return internalSSLSocketFactory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket())
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort))
    }

    private fun enableTLSOnSocket(socket: Socket?): Socket? {
        if (socket is SSLSocket) {
            val supportedProtocols = socket.supportedProtocols.toSet()
            val filteredEnabledProtocols = enabledProtocols.filter { it in supportedProtocols }

            if (filteredEnabledProtocols.isNotEmpty()) {
                try {
                    socket.enabledProtocols = filteredEnabledProtocols.toTypedArray()
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
        return socket
    }
}
