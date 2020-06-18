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

package im.vector.matrix.android.internal.legacy.riot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import okhttp3.TlsVersion;
import timber.log.Timber;

/*
 * IMPORTANT: This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 */

/**
 * Force the usage of Tls versions on every created socket
 * Inspired from https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
 */
/*package*/ class TLSSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory internalSSLSocketFactory;

    private String[] enabledProtocols;

    /**
     * Constructor
     *
     * @param trustPinned
     * @param acceptedTlsVersions
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    /*package*/ TLSSocketFactory(TrustManager[] trustPinned, List<TlsVersion> acceptedTlsVersions) throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustPinned, new SecureRandom());
        internalSSLSocketFactory = context.getSocketFactory();

        enabledProtocols = new String[acceptedTlsVersions.size()];
        int i = 0;
        for (TlsVersion tlsVersion : acceptedTlsVersions) {
            enabledProtocols[i] = tlsVersion.javaName();
            i++;
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket != null && (socket instanceof SSLSocket)) {
            SSLSocket sslSocket = (SSLSocket) socket;

            List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
            List<String> filteredEnabledProtocols = new ArrayList<>();

            for (String protocol : enabledProtocols) {
                if (supportedProtocols.contains(protocol)) {
                    filteredEnabledProtocols.add(protocol);
                }
            }

            if (!filteredEnabledProtocols.isEmpty()) {
                try {
                    sslSocket.setEnabledProtocols(filteredEnabledProtocols.toArray(new String[filteredEnabledProtocols.size()]));
                } catch (Exception e) {
                    Timber.e(e, "Exception");
                }
            }
        }
        return socket;
    }
}
