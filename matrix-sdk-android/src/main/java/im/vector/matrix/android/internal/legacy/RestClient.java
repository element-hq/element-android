/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import im.vector.matrix.android.BuildConfig;
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.auth.data.Credentials;
import im.vector.matrix.android.internal.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.listeners.IMXNetworkEventListener;
import im.vector.matrix.android.internal.legacy.network.NetworkConnectivityReceiver;
import im.vector.matrix.android.internal.legacy.rest.client.MXRestExecutorService;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.PolymorphicRequestBodyConverter;
import im.vector.matrix.android.internal.legacy.util.UnsentEventsManager;
import im.vector.matrix.android.internal.network.ssl.CertUtil;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * Class for making Matrix API calls.
 */
public class RestClient<T> {

    public static final String URI_API_PREFIX_PATH_MEDIA_R0 = "_matrix/media/r0/";
    public static final String URI_API_PREFIX_PATH_MEDIA_PROXY_UNSTABLE = "_matrix/media_proxy/unstable/";
    public static final String URI_API_PREFIX_PATH = "_matrix/client/";
    public static final String URI_API_PREFIX_PATH_R0 = "_matrix/client/r0/";
    public static final String URI_API_PREFIX_PATH_UNSTABLE = "_matrix/client/unstable/";

    /**
     * Prefix used in path of identity server API requests.
     */
    public static final String URI_API_PREFIX_IDENTITY = "_matrix/identity/api/v1/";

    /**
     * List the servers which should be used to define the base url.
     */
    public enum EndPointServer {
        HOME_SERVER,
        IDENTITY_SERVER,
        ANTIVIRUS_SERVER
    }

    protected static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final int WRITE_TIMEOUT_MS = 60000;

    protected Credentials mCredentials;

    protected T mApi;

    protected Gson gson;

    protected UnsentEventsManager mUnsentEventsManager;

    protected HomeServerConnectionConfig mHsConfig;

    // unitary tests only
    public static boolean mUseMXExecutor = false;

    // the user agent
    private static String sUserAgent = null;

    // http client
    private OkHttpClient mOkHttpClient = new OkHttpClient();

    public RestClient(SessionParams sessionParams, Class<T> type, String uriPrefix, boolean withNullSerialization) {
        this(sessionParams, type, uriPrefix, withNullSerialization, EndPointServer.HOME_SERVER);
    }

    /**
     * Public constructor.
     *
     * @param sessionParams         the session data
     * @param type                  the REST type
     * @param uriPrefix             the URL request prefix
     * @param withNullSerialization true to serialise class member with null value
     * @param useIdentityServer     true to use the identity server URL as base request
     */
    public RestClient(SessionParams sessionParams, Class<T> type, String uriPrefix, boolean withNullSerialization, boolean useIdentityServer) {
        this(sessionParams, type, uriPrefix, withNullSerialization, useIdentityServer ? EndPointServer.IDENTITY_SERVER : EndPointServer.HOME_SERVER);
    }

    /**
     * Public constructor.
     *
     * @param sessionParams         the session data
     * @param type                  the REST type
     * @param uriPrefix             the URL request prefix
     * @param withNullSerialization true to serialise class member with null value
     * @param endPointServer        tell which server is used to define the base url
     */
    public RestClient(SessionParams sessionParams, Class<T> type, String uriPrefix, boolean withNullSerialization, EndPointServer endPointServer) {
        // The JSON -> object mapper
        gson = JsonUtils.getGson(withNullSerialization);
        mHsConfig = sessionParams.getHomeServerConnectionConfig();
        mCredentials = sessionParams.getCredentials();

        Interceptor authentInterceptor = new Interceptor() {

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Request.Builder newRequestBuilder = request.newBuilder();
                if (null != sUserAgent) {
                    // set a custom user agent
                    newRequestBuilder.addHeader("User-Agent", sUserAgent);
                }
                // Add the access token to all requests if it is set
                if (mCredentials != null) {
                    newRequestBuilder.addHeader("Authorization", "Bearer " + mCredentials.getAccessToken());
                }
                request = newRequestBuilder.build();

                return chain.proceed(request);
            }
        };

        // TODO Remove this, seems so useless
        Interceptor connectivityInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                if (mUnsentEventsManager != null
                        && mUnsentEventsManager.getNetworkConnectivityReceiver() != null
                        && !mUnsentEventsManager.getNetworkConnectivityReceiver().isConnected()) {
                    throw new IOException("Not connected");
                }
                return chain.proceed(chain.request());
            }
        };

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(authentInterceptor)
                .addInterceptor(connectivityInterceptor);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            okHttpClientBuilder
                    .addInterceptor(loggingInterceptor);
        }


        if (mUseMXExecutor) {
            okHttpClientBuilder.dispatcher(new Dispatcher(new MXRestExecutorService()));
        }

        try {
            Pair<SSLSocketFactory, X509TrustManager> pair = CertUtil.INSTANCE.newPinnedSSLSocketFactory(mHsConfig);
            okHttpClientBuilder.sslSocketFactory(pair.first, pair.second);
            okHttpClientBuilder.hostnameVerifier(CertUtil.INSTANCE.newHostnameVerifier(mHsConfig));
            okHttpClientBuilder.connectionSpecs(CertUtil.INSTANCE.newConnectionSpecs(mHsConfig));
        } catch (Exception e) {
            Timber.e("## RestClient() setSslSocketFactory failed" + e.getMessage(), e);
        }
        mOkHttpClient = okHttpClientBuilder.build();
        final String endPoint = makeEndpoint(mHsConfig, uriPrefix, endPointServer);
        // Rest adapter for turning API interfaces into actual REST-calling objects
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(endPoint)
                .addConverterFactory(PolymorphicRequestBodyConverter.FACTORY)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(mOkHttpClient);

        Retrofit retrofit = builder.build();

        mApi = retrofit.create(type);
    }

    @NonNull
    private String makeEndpoint(HomeServerConnectionConfig hsConfig, String uriPrefix, EndPointServer endPointServer) {
        String baseUrl;
        switch (endPointServer) {
            case IDENTITY_SERVER:
                baseUrl = hsConfig.getIdentityServerUri().toString();
                break;
            case ANTIVIRUS_SERVER:
                baseUrl = hsConfig.getAntiVirusServerUri().toString();
                break;
            case HOME_SERVER:
            default:
                baseUrl = hsConfig.getHomeServerUri().toString();

        }
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base url shouldn't be null");
        }
        baseUrl = sanitizeBaseUrl(baseUrl);
        String dynamicPath = sanitizeDynamicPath(uriPrefix);
        return baseUrl + dynamicPath;
    }

    private String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl;
        }
        return baseUrl + "/";
    }

    private String sanitizeDynamicPath(String dynamicPath) {
        // remove any trailing http in the uri prefix
        if (dynamicPath.startsWith("http://")) {
            dynamicPath = dynamicPath.substring("http://".length());
        } else if (dynamicPath.startsWith("https://")) {
            dynamicPath = dynamicPath.substring("https://".length());
        }
        return dynamicPath;
    }

    /**
     * Create an user agent with the application version.
     * Ex: Riot/0.8.12 (Linux; U; Android 6.0.1; SM-A510F Build/MMB29; Flavour FDroid; MatrixAndroidSDK 0.9.6)
     *
     * @param appContext the application context
     */
    public static void initUserAgent(Context appContext) {
        String appName = "";
        String appVersion = "";

        if (null != appContext) {
            try {
                PackageManager pm = appContext.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(appContext.getApplicationContext().getPackageName(), 0);
                appName = pm.getApplicationLabel(appInfo).toString();

                PackageInfo pkgInfo = pm.getPackageInfo(appContext.getApplicationContext().getPackageName(), 0);
                appVersion = pkgInfo.versionName;
            } catch (Exception e) {
                Timber.e("## initUserAgent() : failed " + e.getMessage(), e);
            }
        }

        sUserAgent = System.getProperty("http.agent");

        // cannot retrieve the application version
        if (TextUtils.isEmpty(appName) || TextUtils.isEmpty(appVersion)) {
            if (null == sUserAgent) {
                sUserAgent = "Java" + System.getProperty("java.version");
            }
            return;
        }

        // if there is no user agent or cannot parse it
        if ((null == sUserAgent) || (sUserAgent.lastIndexOf(")") == -1) || (sUserAgent.indexOf("(") == -1)) {
            sUserAgent = appName + "/" + appVersion + "; MatrixAndroidSDK " + BuildConfig.VERSION_NAME + ")";
        } else {
            // update
            sUserAgent = appName + "/" + appVersion + " " +
                    sUserAgent.substring(sUserAgent.indexOf("("), sUserAgent.lastIndexOf(")") - 1) +
                    "; MatrixAndroidSDK " + BuildConfig.VERSION_NAME + ")";
        }
    }

    /**
     * Get the current user agent
     *
     * @return the current user agent, or null in case of error or if not initialized yet
     */
    @Nullable
    public static String getUserAgent() {
        return sUserAgent;
    }

    /**
     * Refresh the connection timeouts.
     *
     * @param networkConnectivityReceiver the network connectivity receiver
     */
    private void refreshConnectionTimeout(NetworkConnectivityReceiver networkConnectivityReceiver) {
        OkHttpClient.Builder builder = mOkHttpClient.newBuilder();

        if (networkConnectivityReceiver.isConnected()) {
            float factor = networkConnectivityReceiver.getTimeoutScale();

            builder
                    .connectTimeout((int) (CONNECTION_TIMEOUT_MS * factor), TimeUnit.MILLISECONDS)
                    .readTimeout((int) (READ_TIMEOUT_MS * factor), TimeUnit.MILLISECONDS)
                    .writeTimeout((int) (WRITE_TIMEOUT_MS * factor), TimeUnit.MILLISECONDS);

            Timber.d("## refreshConnectionTimeout()  : update setConnectTimeout to " + (CONNECTION_TIMEOUT_MS * factor) + " ms");
            Timber.d("## refreshConnectionTimeout()  : update setReadTimeout to " + (READ_TIMEOUT_MS * factor) + " ms");
            Timber.d("## refreshConnectionTimeout()  : update setWriteTimeout to " + (WRITE_TIMEOUT_MS * factor) + " ms");
        } else {
            builder.connectTimeout(1, TimeUnit.MILLISECONDS);
            Timber.d("## refreshConnectionTimeout()  : update the requests timeout to 1 ms");
        }

        // FIXME It has no effect to the rest client
        mOkHttpClient = builder.build();
    }

    /**
     * Update the connection timeout
     *
     * @param aTimeoutMs the connection timeout
     */
    protected void setConnectionTimeout(int aTimeoutMs) {
        int timeoutMs = aTimeoutMs;

        if (null != mUnsentEventsManager) {
            NetworkConnectivityReceiver networkConnectivityReceiver = mUnsentEventsManager.getNetworkConnectivityReceiver();

            if (null != networkConnectivityReceiver) {
                if (networkConnectivityReceiver.isConnected()) {
                    timeoutMs *= networkConnectivityReceiver.getTimeoutScale();
                } else {
                    timeoutMs = 1000;
                }
            }
        }

        if (timeoutMs != mOkHttpClient.connectTimeoutMillis()) {
            // FIXME It has no effect to the rest client
            mOkHttpClient = mOkHttpClient.newBuilder().connectTimeout(timeoutMs, TimeUnit.MILLISECONDS).build();
        }
    }

    /**
     * Set the unsentEvents manager.
     *
     * @param unsentEventsManager The unsentEvents manager.
     */
    public void setUnsentEventsManager(UnsentEventsManager unsentEventsManager) {
        mUnsentEventsManager = unsentEventsManager;

        final NetworkConnectivityReceiver networkConnectivityReceiver = mUnsentEventsManager.getNetworkConnectivityReceiver();
        refreshConnectionTimeout(networkConnectivityReceiver);

        networkConnectivityReceiver.addEventListener(new IMXNetworkEventListener() {
            @Override
            public void onNetworkConnectionUpdate(boolean isConnected) {
                Timber.d("## setUnsentEventsManager()  : update the requests timeout to " + (isConnected ? CONNECTION_TIMEOUT_MS : 1) + " ms");
                refreshConnectionTimeout(networkConnectivityReceiver);
            }
        });
    }

    /**
     * Get the user's getCredentials. Typically for saving them somewhere persistent.
     *
     * @return the user getCredentials
     */
    public Credentials getCredentials() {
        return mCredentials;
    }

    /**
     * Provide the user's getCredentials. To be called after login or registration.
     *
     * @param credentials the user getCredentials
     */
    public void setCredentials(Credentials credentials) {
        mCredentials = credentials;
    }

    /**
     * Default protected constructor for unit tests.
     */
    protected RestClient() {
    }

    /**
     * Protected setter for injection by unit tests.
     *
     * @param api the api object
     */
    @VisibleForTesting()
    protected void setApi(T api) {
        mApi = api;
    }
}
