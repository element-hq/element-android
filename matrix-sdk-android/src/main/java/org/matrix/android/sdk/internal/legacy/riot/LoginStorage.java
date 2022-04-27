/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.legacy.riot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 *
 * Stores login credentials in SharedPreferences.
 */
public class LoginStorage {
    private static final String PREFS_LOGIN = "Vector.LoginStorage";

    // multi accounts + homeserver config
    private static final String PREFS_KEY_CONNECTION_CONFIGS = "PREFS_KEY_CONNECTION_CONFIGS";

    private final Context mContext;

    public LoginStorage(Context appContext) {
        mContext = appContext.getApplicationContext();

    }

    /**
     * @return the list of homeserver configurations.
     */
    public List<HomeServerConnectionConfig> getCredentialsList() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);

        String connectionConfigsString = prefs.getString(PREFS_KEY_CONNECTION_CONFIGS, null);

        Timber.d("Got connection json: ");

        if (connectionConfigsString == null) {
            return new ArrayList<>();
        }

        try {
            JSONArray connectionConfigsStrings = new JSONArray(connectionConfigsString);

            List<HomeServerConnectionConfig> configList = new ArrayList<>(
                    connectionConfigsStrings.length()
            );

            for (int i = 0; i < connectionConfigsStrings.length(); i++) {
                configList.add(
                        HomeServerConnectionConfig.fromJson(connectionConfigsStrings.getJSONObject(i))
                );
            }

            return configList;
        } catch (JSONException e) {
            Timber.e(e, "Failed to deserialize accounts");
            throw new RuntimeException("Failed to deserialize accounts");
        }
    }

    /**
     * Add a credentials to the credentials list
     *
     * @param config the homeserver config to add.
     */
    public void addCredentials(HomeServerConnectionConfig config) {
        if (null != config && config.getCredentials() != null) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            List<HomeServerConnectionConfig> configs = getCredentialsList();

            configs.add(config);

            List<JSONObject> serialized = new ArrayList<>(configs.size());

            try {
                for (HomeServerConnectionConfig c : configs) {
                    serialized.add(c.toJson());
                }
            } catch (JSONException e) {
                throw new RuntimeException("Failed to serialize connection config");
            }

            String ser = new JSONArray(serialized).toString();

            Timber.d("Storing " + serialized.size() + " credentials");

            editor.putString(PREFS_KEY_CONNECTION_CONFIGS, ser);
            editor.apply();
        }
    }

    /**
     * Remove the credentials from credentials list
     *
     * @param config the credentials to remove
     */
    public void removeCredentials(HomeServerConnectionConfig config) {
        if (null != config && config.getCredentials() != null) {
            Timber.d("Removing account: " + config.getCredentials().userId);

            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            List<HomeServerConnectionConfig> configs = getCredentialsList();
            List<JSONObject> serialized = new ArrayList<>(configs.size());

            boolean found = false;
            try {
                for (HomeServerConnectionConfig c : configs) {
                    if (c.getCredentials().userId.equals(config.getCredentials().userId)) {
                        found = true;
                    } else {
                        serialized.add(c.toJson());
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException("Failed to serialize connection config");
            }

            if (!found) return;

            String ser = new JSONArray(serialized).toString();

            Timber.d("Storing " + serialized.size() + " credentials");

            editor.putString(PREFS_KEY_CONNECTION_CONFIGS, ser);
            editor.apply();
        }
    }

    /**
     * Replace the credential from credentials list, based on credentials.userId.
     * If it does not match an existing credential it does *not* insert the new credentials.
     *
     * @param config the credentials to insert
     */
    public void replaceCredentials(HomeServerConnectionConfig config) {
        if (null != config && config.getCredentials() != null) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            List<HomeServerConnectionConfig> configs = getCredentialsList();
            List<JSONObject> serialized = new ArrayList<>(configs.size());

            boolean found = false;
            try {
                for (HomeServerConnectionConfig c : configs) {
                    if (c.getCredentials().userId.equals(config.getCredentials().userId)) {
                        serialized.add(config.toJson());
                        found = true;
                    } else {
                        serialized.add(c.toJson());
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException("Failed to serialize connection config");
            }

            if (!found) return;

            String ser = new JSONArray(serialized).toString();

            Timber.d("Storing " + serialized.size() + " credentials");

            editor.putString(PREFS_KEY_CONNECTION_CONFIGS, ser);
            editor.apply();
        }
    }

    /**
     * Clear the stored values
     */
    @SuppressLint("ApplySharedPref")
    public void clear() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_LOGIN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREFS_KEY_CONNECTION_CONFIGS);
        //Need to commit now because called before forcing an app restart
        editor.commit();
    }
}
