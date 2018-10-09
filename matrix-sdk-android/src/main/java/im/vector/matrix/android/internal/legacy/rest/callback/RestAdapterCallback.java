/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.callback;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import im.vector.matrix.android.internal.legacy.rest.model.HttpError;
import im.vector.matrix.android.internal.legacy.rest.model.HttpException;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.UnsentEventsManager;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestAdapterCallback<T> implements Callback<T> {

    private static final String LOG_TAG = "RestAdapterCallback";

    /**
     * Callback when a request failed after a network error.
     * This callback should manage the request auto resent.
     */
    public interface RequestRetryCallBack {
        void onRetry();
    }

    // the event description
    private final String mEventDescription;

    // the callback
    // FIXME It should be safer if the type was ApiCallback<T>, else onSuccess() has to be overridden
    private final ApiCallback mApiCallback;

    // the retry callback
    private final RequestRetryCallBack mRequestRetryCallBack;

    // the unsent events manager
    private final UnsentEventsManager mUnsentEventsManager;

    // true to do not test if he event time line when sending again
    // the request when a data connection is retrieved.
    private final boolean mIgnoreEventTimeLifeInOffline;

    /**
     * Constructor with unsent events management
     *
     * @param description          the event description
     * @param unsentEventsManager  the unsent events manager
     * @param apiCallback          the callback
     * @param requestRetryCallBack the retry callback
     */
    public RestAdapterCallback(String description,
                               UnsentEventsManager unsentEventsManager,
                               ApiCallback apiCallback,
                               RequestRetryCallBack requestRetryCallBack) {
        this(description, unsentEventsManager, false, apiCallback, requestRetryCallBack);
    }

    /**
     * Constructor with unsent events management
     *
     * @param description                the event description
     * @param ignoreEventTimeLifeOffline true to ignore the event time when resending the event.
     * @param unsentEventsManager        the unsent events manager
     * @param apiCallback                the callback
     * @param requestRetryCallBack       the retry callback
     */
    public RestAdapterCallback(String description,
                               UnsentEventsManager unsentEventsManager,
                               boolean ignoreEventTimeLifeOffline,
                               ApiCallback apiCallback,
                               RequestRetryCallBack requestRetryCallBack) {
        if (null != description) {
            Log.d(LOG_TAG, "Trigger the event [" + description + "]");
        }

        mEventDescription = description;
        mIgnoreEventTimeLifeInOffline = ignoreEventTimeLifeOffline;
        mApiCallback = apiCallback;
        mRequestRetryCallBack = requestRetryCallBack;
        mUnsentEventsManager = unsentEventsManager;
    }

    /**
     * Notify the {@link UnsentEventsManager} that the event has been successfully sent.
     * This method must be called each time a REST call succeed, in order to warn
     * the {@link UnsentEventsManager} to send the next unsent events.
     */
    protected void onEventSent() {
        if (null != mUnsentEventsManager) {
            try {
                // some users reported that their devices were connected
                // whereas this receiver was not called
                if (!mUnsentEventsManager.getNetworkConnectivityReceiver().isConnected()) {
                    Log.d(LOG_TAG, "## onEventSent(): request succeed, while network seen as disconnected => ask ConnectivityReceiver to dispatch info");
                    mUnsentEventsManager.getNetworkConnectivityReceiver().checkNetworkConnection(mUnsentEventsManager.getContext());
                }

                mUnsentEventsManager.onEventSent(mApiCallback);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onEventSent(): Exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onResponse(Call<T> call, final Response<T> response) {
        try {
            handleResponse(response);
        } catch (IOException e) {
            onFailure(call, e);
        }
    }

    private void handleResponse(final Response<T> response) throws IOException {
        DefaultRetrofit2ResponseHandler.handleResponse(
                response,
                new DefaultRetrofit2ResponseHandler.Listener<T>() {
                    @Override
                    public void onSuccess(Response<T> response) {
                        success(response.body(), response);
                    }

                    @Override
                    public void onHttpError(HttpError httpError) {
                        failure(response, new HttpException(httpError));
                    }
                }
        );
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        failure(null, (Exception) t);
    }

    public void success(T t, Response<T> response) {
        if (null != mEventDescription) {
            Log.d(LOG_TAG, "## Succeed() : [" + mEventDescription + "]");
        }

        // add try catch to prevent application crashes while managing destroyed object
        try {
            onEventSent();

            if (null != mApiCallback) {
                try {
                    mApiCallback.onSuccess(t);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## succeed() : onSuccess failed " + e.getMessage(), e);
                    mApiCallback.onUnexpectedError(e);
                }
            }
        } catch (Exception e) {
            // privacy
            Log.e(LOG_TAG, "## succeed(): Exception " + e.getMessage(), e);
        }
    }

    /**
     * Default failure implementation that calls the right error handler
     *
     * @param response  the retrofit response
     * @param exception the retrofit exception
     */
    public void failure(Response<T> response, Exception exception) {
        if (null != mEventDescription) {
            String error = exception != null
                    ? exception.getMessage()
                    : (response != null ? response.message() : "unknown");

            Log.d(LOG_TAG, "## failure(): [" + mEventDescription + "]" + " with error " + error);
        }

        boolean retry = true;

        if (null != response) {
            retry = (response.code() < 400) || (response.code() > 500);
        }

        // do not retry if the response format is not the expected one.
        retry &= (null == exception.getCause())
                || !(exception.getCause() instanceof MalformedJsonException || exception.getCause() instanceof JsonSyntaxException);

        if (retry && (null != mUnsentEventsManager)) {
            Log.d(LOG_TAG, "Add it to the UnsentEventsManager");
            mUnsentEventsManager.onEventSendingFailed(mEventDescription, mIgnoreEventTimeLifeInOffline, response, exception, mApiCallback,
                    mRequestRetryCallBack);
        } else {
            if (exception != null && exception instanceof IOException) {
                try {
                    if (null != mApiCallback) {
                        try {
                            mApiCallback.onNetworkError(exception);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "## failure(): onNetworkError " + exception.getLocalizedMessage(), exception);
                        }
                    }
                } catch (Exception e) {
                    // privacy
                    //Log.e(LOG_TAG, "Exception NetworkError " + e.getMessage() + " while managing " + error.getUrl());
                    Log.e(LOG_TAG, "## failure():  NetworkError " + e.getMessage(), e);
                }
            } else {
                // Try to convert this into a Matrix error
                MatrixError mxError;
                try {
                    HttpError error = ((HttpException) exception).getHttpError();
                    ResponseBody errorBody = response.errorBody();

                    String bodyAsString = error.getErrorBody();
                    mxError = JsonUtils.getGson(false).fromJson(bodyAsString, MatrixError.class);

                    mxError.mStatus = response.code();
                    mxError.mReason = response.message();
                    mxError.mErrorBodyMimeType = errorBody.contentType();
                    mxError.mErrorBody = errorBody;
                    mxError.mErrorBodyAsString = bodyAsString;
                } catch (Exception e) {
                    mxError = null;
                }
                if (mxError != null) {
                    if (MatrixError.LIMIT_EXCEEDED.equals(mxError.errcode) && (null != mUnsentEventsManager)) {
                        mUnsentEventsManager.onEventSendingFailed(mEventDescription, mIgnoreEventTimeLifeInOffline, response, exception, mApiCallback,
                                mRequestRetryCallBack);
                    } else if (MatrixError.isConfigurationErrorCode(mxError.errcode) && (null != mUnsentEventsManager)) {
                        mUnsentEventsManager.onConfigurationErrorCode(mxError.errcode, mEventDescription);
                    } else {
                        try {
                            if (null != mApiCallback) {
                                mApiCallback.onMatrixError(mxError);
                            }
                        } catch (Exception e) {
                            // privacy
                            //Log.e(LOG_TAG, "Exception MatrixError " + e.getMessage() + " while managing " + error.getUrl());
                            Log.e(LOG_TAG, "## failure():  MatrixError " + e.getMessage(), e);
                        }
                    }
                } else {
                    try {
                        if (null != mApiCallback) {
                            mApiCallback.onUnexpectedError(exception);
                        }
                    } catch (Exception e) {
                        // privacy
                        //Log.e(LOG_TAG, "Exception UnexpectedError " + e.getMessage() + " while managing " + error.getUrl());
                        Log.e(LOG_TAG, "## failure():  UnexpectedError " + e.getMessage(), e);
                    }
                }
            }
        }
    }
}
