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
package im.vector.matrix.android.internal.legacy.rest.model.login;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.rest.client.LoginRestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Object to pass to a /login call of type password.
 */
public class PasswordLoginParams extends LoginParams {
    public static final String IDENTIFIER_KEY_TYPE_USER = "m.id.user";
    public static final String IDENTIFIER_KEY_TYPE_THIRD_PARTY = "m.id.thirdparty";
    public static final String IDENTIFIER_KEY_TYPE_PHONE = "m.id.phone";

    public static final String IDENTIFIER_KEY_TYPE = "type";
    public static final String IDENTIFIER_KEY_MEDIUM = "medium";
    public static final String IDENTIFIER_KEY_ADDRESS = "address";
    public static final String IDENTIFIER_KEY_USER = "user";
    public static final String IDENTIFIER_KEY_COUNTRY = "country";
    public static final String IDENTIFIER_KEY_NUMBER = "number";

    // identifier parameters
    public Map<String, Object> identifier;

    // user name login
    public String user;

    // email login
    public String address;
    public String medium;

    // common
    public String password;

    // A display name to assign to the newly-created device
    public String initial_device_display_name;

    // The device id, used for e2e encryption
    public String device_id;

    /**
     * Set login params for username/password
     *
     * @param username the username
     * @param password the password
     */
    public void setUserIdentifier(@NonNull final String username, @NonNull final String password) {
        identifier = new HashMap<>();
        identifier.put(IDENTIFIER_KEY_TYPE, IDENTIFIER_KEY_TYPE_USER);
        identifier.put(IDENTIFIER_KEY_USER, username);
        // For backward compatibility
        user = username;

        setOtherData(password);
    }

    /**
     * Set login params for 3pid(except phone number)/password
     *
     * @param medium   3pid type
     * @param address  3pid value
     * @param password the password
     */
    public void setThirdPartyIdentifier(@NonNull final String medium, @NonNull final String address, @NonNull final String password) {
        identifier = new HashMap<>();
        identifier.put(IDENTIFIER_KEY_TYPE, IDENTIFIER_KEY_TYPE_THIRD_PARTY);
        identifier.put(IDENTIFIER_KEY_MEDIUM, medium);
        identifier.put(IDENTIFIER_KEY_ADDRESS, address);
        // For backward compatibility
        this.medium = medium;
        this.address = address;

        setOtherData(password);
    }

    /**
     * Set login params for phone number/password
     *
     * @param phoneNumber the phone number
     * @param countryCode the country code
     * @param password    the password
     */
    public void setPhoneIdentifier(@NonNull final String phoneNumber, @NonNull final String countryCode, @NonNull final String password) {
        identifier = new HashMap<>();
        identifier.put(IDENTIFIER_KEY_TYPE, IDENTIFIER_KEY_TYPE_PHONE);
        identifier.put(IDENTIFIER_KEY_NUMBER, phoneNumber);
        identifier.put(IDENTIFIER_KEY_COUNTRY, countryCode);

        setOtherData(password);
    }

    /**
     * Set basic params
     *
     * @param password the password
     */
    private void setOtherData(@NonNull final String password) {
        this.password = password;
        type = LoginRestClient.LOGIN_FLOW_TYPE_PASSWORD;
        initial_device_display_name = Build.MODEL.trim();
    }

    /**
     * Set the device name
     *
     * @param deviceName the new device name
     */
    public void setDeviceName(String deviceName) {
        if ((null != deviceName) && !TextUtils.isEmpty(deviceName.trim())) {
            initial_device_display_name = deviceName.trim();
        } else {
            initial_device_display_name = Build.MODEL.trim();
        }
    }

    /**
     * Set the device Id
     *
     * @param deviceId the device id, used for e2e encryption
     */
    public void setDeviceId(String deviceId) {
        device_id = deviceId;
    }
}
