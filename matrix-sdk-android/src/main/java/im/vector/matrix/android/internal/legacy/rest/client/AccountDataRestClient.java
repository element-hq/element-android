/*
 * Copyright 2015 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.client;

import java.util.HashMap;
import java.util.Map;

import im.vector.matrix.android.api.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.AccountDataApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;

public class AccountDataRestClient extends RestClient<AccountDataApi> {
    /**
     * Account data types
     */
    public static final String ACCOUNT_DATA_TYPE_IGNORED_USER_LIST = "m.ignored_user_list";
    public static final String ACCOUNT_DATA_TYPE_DIRECT_MESSAGES = "m.direct";
    public static final String ACCOUNT_DATA_TYPE_PREVIEW_URLS = "org.matrix.preview_urls";
    public static final String ACCOUNT_DATA_TYPE_WIDGETS = "m.widgets";

    /**
     * Account data keys
     */
    public static final String ACCOUNT_DATA_KEY_IGNORED_USERS = "ignored_users";
    public static final String ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE = "disable";

    /**
     * {@inheritDoc}
     */
    public AccountDataRestClient(SessionParams sessionParams) {
        super(sessionParams, AccountDataApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    /**
     * Set some account_data for the client.
     *
     * @param userId   the user id
     * @param type     the account data type.
     * @param params   the put params.
     * @param callback the asynchronous callback called when finished
     */
    public void setAccountData(final String userId, final String type, final Object params, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "setAccountData userId : " + userId + " type " + type + " params " + params;
        final String description = "setAccountData userId : " + userId + " type " + type;

        mApi.setAccountData(userId, type, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        setAccountData(userId, type, params, callback);
                    }
                }));
    }

    /**
     * Gets a bearer token from the homeserver that the user can
     * present to a third party in order to prove their ownership
     * of the Matrix account they are logged into.
     *
     * @param userId   the user id
     * @param callback the asynchronous callback called when finished
     */
    public void openIdToken(final String userId, final ApiCallback<Map<Object, Object>> callback) {
        final String description = "openIdToken userId : " + userId;

        mApi.openIdToken(userId, new HashMap<>())
                .enqueue(new RestAdapterCallback<Map<Object, Object>>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                openIdToken(userId, callback);
                            }
                        }));
    }
}
