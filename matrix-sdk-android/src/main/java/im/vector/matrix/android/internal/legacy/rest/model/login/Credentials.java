/*
 * Copyright 2014 OpenMarket Ltd
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

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The user's getCredentials.
 */
public class Credentials {
    public String userId;
    public String homeServer;  // This is the server name and not a URI, e.g. "matrix.org"
    public String accessToken;
    public String refreshToken;
    public String deviceId;

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("user_id", userId);
        json.put("home_server", homeServer);
        json.put("access_token", accessToken);
        json.put("refresh_token", TextUtils.isEmpty(refreshToken) ? JSONObject.NULL : refreshToken);
        json.put("device_id", deviceId);

        return json;
    }

    public static Credentials fromJson(JSONObject obj) throws JSONException {
        Credentials creds = new Credentials();
        creds.userId = obj.getString("user_id");
        creds.homeServer = obj.getString("home_server");
        creds.accessToken = obj.getString("access_token");

        if (obj.has("device_id")) {
            creds.deviceId = obj.getString("device_id");
        }

        // refresh_token is mandatory
        if (obj.has("refresh_token")) {
            try {
                creds.refreshToken = obj.getString("refresh_token");
            } catch (Exception e) {
                creds.refreshToken = null;
            }
        } else {
            throw new RuntimeException("refresh_token is required.");
        }

        return creds;
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "userId='" + userId + '\'' +
                ", homeServer='" + homeServer + '\'' +
                ", refreshToken.length='" + (refreshToken != null ? refreshToken.length() : "null") + '\'' +
                ", accessToken.length='" + (accessToken != null ? accessToken.length() : "null") + '\'' +
                '}';
    }
}
