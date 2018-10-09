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
package im.vector.matrix.android.internal.legacy.rest.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

/**
 * Represents a standard error response.
 */
public class MatrixError implements java.io.Serializable {
    public static final String FORBIDDEN = "M_FORBIDDEN";
    public static final String UNKNOWN = "M_UNKNOWN";
    public static final String UNKNOWN_TOKEN = "M_UNKNOWN_TOKEN";
    public static final String BAD_JSON = "M_BAD_JSON";
    public static final String NOT_JSON = "M_NOT_JSON";
    public static final String NOT_FOUND = "M_NOT_FOUND";
    public static final String LIMIT_EXCEEDED = "M_LIMIT_EXCEEDED";
    public static final String USER_IN_USE = "M_USER_IN_USE";
    public static final String ROOM_IN_USE = "M_ROOM_IN_USE";
    public static final String BAD_PAGINATION = "M_BAD_PAGINATION";
    public static final String UNAUTHORIZED = "M_UNAUTHORIZED";
    public static final String OLD_VERSION = "M_OLD_VERSION";
    public static final String UNRECOGNIZED = "M_UNRECOGNIZED";

    public static final String LOGIN_EMAIL_URL_NOT_YET = "M_LOGIN_EMAIL_URL_NOT_YET";
    public static final String THREEPID_AUTH_FAILED = "M_THREEPID_AUTH_FAILED";
    // Error code returned by the server when no account matches the given 3pid
    public static final String THREEPID_NOT_FOUND = "M_THREEPID_NOT_FOUND";
    public static final String THREEPID_IN_USE = "M_THREEPID_IN_USE";
    public static final String SERVER_NOT_TRUSTED = "M_SERVER_NOT_TRUSTED";
    public static final String TOO_LARGE = "M_TOO_LARGE";
    public static final String M_CONSENT_NOT_GIVEN = "M_CONSENT_NOT_GIVEN";
    public static final String RESOURCE_LIMIT_EXCEEDED = "M_RESOURCE_LIMIT_EXCEEDED";

    // custom ones
    public static final String NOT_SUPPORTED = "M_NOT_SUPPORTED";

    // Possible value for "limit_type"
    public static final String LIMIT_TYPE_MAU = "monthly_active_user";

    // Define the configuration error codes.
    // The others matrix errors are requests dedicated
    // UNKNOWN_TOKEN : the access token is no more valid
    // OLD_VERSION : the current SDK / application versions are too old and might trigger some unexpected errors.
    public static final Set<String> mConfigurationErrorCodes = new HashSet<>(Arrays.asList(UNKNOWN_TOKEN, OLD_VERSION));

    public String errcode;
    public String error;
    public Integer retry_after_ms;

    @SerializedName("consent_uri")
    public String consentUri;

    // RESOURCE_LIMIT_EXCEEDED data
    @SerializedName("limit_type")
    public String limitType;
    @Nullable
    @SerializedName("admin_contact")
    public String adminUri;


    // extracted from the error response
    public Integer mStatus;
    public String mReason;
    public ResponseBody mErrorBody;
    public String mErrorBodyAsString;
    public MediaType mErrorBodyMimeType;

    /**
     * Default creator
     */
    public MatrixError() {
    }

    /**
     * Creator with error description
     *
     * @param anErrcode the error code.
     * @param anError   the error message.
     */
    public MatrixError(String anErrcode, String anError) {
        errcode = anErrcode;
        error = anError;
    }

    /**
     * @return a localized error message.
     */
    public String getLocalizedMessage() {
        String localizedMessage = "";

        if (!TextUtils.isEmpty(error)) {
            localizedMessage = error;
        } else if (!TextUtils.isEmpty(errcode)) {
            localizedMessage = errcode;
        }

        return localizedMessage;
    }

    /**
     * @return a  error message.
     */
    public String getMessage() {
        return getLocalizedMessage();
    }

    /**
     * @return true if the error code is a supported one
     */
    public boolean isSupportedErrorCode() {
        return MatrixError.FORBIDDEN.equals(errcode)
                || MatrixError.UNKNOWN_TOKEN.equals(errcode)
                || MatrixError.BAD_JSON.equals(errcode)
                || MatrixError.NOT_JSON.equals(errcode)
                || MatrixError.NOT_FOUND.equals(errcode)
                || MatrixError.LIMIT_EXCEEDED.equals(errcode)
                || MatrixError.USER_IN_USE.equals(errcode)
                || MatrixError.ROOM_IN_USE.equals(errcode)
                || MatrixError.TOO_LARGE.equals(errcode)
                || MatrixError.BAD_PAGINATION.equals(errcode)
                || MatrixError.OLD_VERSION.equals(errcode)
                || MatrixError.UNRECOGNIZED.equals(errcode)
                || MatrixError.RESOURCE_LIMIT_EXCEEDED.equals(errcode);
    }

    /**
     * Tells if a matrix error code is a configuration error code.
     *
     * @param matrixErrorCode the matrix error code
     * @return true if it is one
     */
    public static boolean isConfigurationErrorCode(String matrixErrorCode) {
        return (null != matrixErrorCode) && mConfigurationErrorCodes.contains(matrixErrorCode);
    }
}
