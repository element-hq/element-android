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

import im.vector.matrix.android.internal.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.data.Pusher;
import im.vector.matrix.android.internal.legacy.rest.api.PushersApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;
import im.vector.matrix.android.internal.legacy.rest.model.PushersResponse;

/**
 * REST client for the Pushers API.
 */
public class PushersRestClient extends RestClient<PushersApi> {
    private static final String LOG_TAG = PushersRestClient.class.getSimpleName();

    private static final String PUSHER_KIND_HTTP = "http";
    private static final String DATA_KEY_HTTP_URL = "url";

    public PushersRestClient(SessionParams sessionParams) {
        super(sessionParams, PushersApi.class, RestClient.URI_API_PREFIX_PATH_R0, true);
    }

    /**
     * Add a new HTTP pusher.
     *
     * @param pushkey           the pushkey
     * @param appId             the application id
     * @param profileTag        the profile tag
     * @param lang              the language
     * @param appDisplayName    a human-readable application name
     * @param deviceDisplayName a human-readable device name
     * @param url               the URL that should be used to send notifications
     * @param append            append the pusher
     * @param withEventIdOnly   true to limit the push content
     * @param callback          the asynchronous callback
     */
    public void addHttpPusher(final String pushkey,
                              final String appId,
                              final String profileTag,
                              final String lang,
                              final String appDisplayName,
                              final String deviceDisplayName,
                              final String url,
                              boolean append,
                              boolean withEventIdOnly,
                              final ApiCallback<Void> callback) {
        manageHttpPusher(pushkey, appId, profileTag, lang, appDisplayName, deviceDisplayName, url, append, withEventIdOnly, true, callback);
    }

    /**
     * remove a new HTTP pusher.
     *
     * @param pushkey           the pushkey
     * @param appId             the application id
     * @param profileTag        the profile tag
     * @param lang              the language
     * @param appDisplayName    a human-readable application name
     * @param deviceDisplayName a human-readable device name
     * @param url               the URL that should be used to send notifications
     * @param callback          the asynchronous callback
     */
    public void removeHttpPusher(final String pushkey,
                                 final String appId,
                                 final String profileTag,
                                 final String lang,
                                 final String appDisplayName,
                                 final String deviceDisplayName,
                                 final String url,
                                 final ApiCallback<Void> callback) {
        manageHttpPusher(pushkey, appId, profileTag, lang, appDisplayName, deviceDisplayName, url, false, false, false, callback);
    }


    /**
     * add/remove a new HTTP pusher.
     *
     * @param pushkey           the pushkey
     * @param appId             the application id
     * @param profileTag        the profile tag
     * @param lang              the language
     * @param appDisplayName    a human-readable application name
     * @param deviceDisplayName a human-readable device name
     * @param url               the URL that should be used to send notifications
     * @param withEventIdOnly   true to limit the push content
     * @param addPusher         true to add the pusher / false to remove it
     * @param callback          the asynchronous callback
     */
    private void manageHttpPusher(final String pushkey,
                                  final String appId,
                                  final String profileTag,
                                  final String lang,
                                  final String appDisplayName,
                                  final String deviceDisplayName,
                                  final String url,
                                  final boolean append,
                                  final boolean withEventIdOnly,
                                  final boolean addPusher,
                                  final ApiCallback<Void> callback) {
        Pusher pusher = new Pusher();
        pusher.pushkey = pushkey;
        pusher.appId = appId;
        pusher.profileTag = profileTag;
        pusher.lang = lang;
        pusher.kind = addPusher ? PUSHER_KIND_HTTP : null;
        pusher.appDisplayName = appDisplayName;
        pusher.deviceDisplayName = deviceDisplayName;
        pusher.data = new HashMap<>();
        pusher.data.put(DATA_KEY_HTTP_URL, url);

        if (addPusher) {
            pusher.append = append;
        }

        if (withEventIdOnly) {
            pusher.data.put("format", "event_id_only");
        }

        final String description = "manageHttpPusher";

        mApi.set(pusher)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        manageHttpPusher(pushkey, appId, profileTag, lang, appDisplayName, deviceDisplayName,
                                url, append, withEventIdOnly, addPusher, callback);
                    }
                }));
    }

    /**
     * Retrieve the pushers list
     *
     * @param callback the callback
     */
    public void getPushers(final ApiCallback<PushersResponse> callback) {
        final String description = "getPushers";

        mApi.get()
                .enqueue(new RestAdapterCallback<PushersResponse>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getPushers(callback);
                    }
                }));
    }
}
