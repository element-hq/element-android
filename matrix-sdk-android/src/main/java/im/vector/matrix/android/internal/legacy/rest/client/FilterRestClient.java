/*
 * Copyright 2018 Matthias Kesler
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

import im.vector.matrix.android.internal.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.FilterApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterBody;
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterResponse;

public class FilterRestClient extends RestClient<FilterApi> {

    /**
     * {@inheritDoc}
     */
    public FilterRestClient(SessionParams sessionParams) {
        super(sessionParams, FilterApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    /**
     * Uploads a FilterBody to homeserver
     *
     * @param userId     the user id
     * @param filterBody FilterBody which should be send to server
     * @param callback   on success callback containing a String with populated filterId
     */
    public void uploadFilter(final String userId, final FilterBody filterBody, final ApiCallback<FilterResponse> callback) {
        final String description = "uploadFilter userId : " + userId + " filter : " + filterBody;

        mApi.uploadFilter(userId, filterBody)
                .enqueue(new RestAdapterCallback<FilterResponse>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        uploadFilter(userId, filterBody, callback);
                    }
                }));
    }

    /**
     * Get a user's filter by filterId
     *
     * @param userId   the user id
     * @param filterId the filter id
     * @param callback on success callback containing a User object with populated filterbody
     */
    public void getFilter(final String userId, final String filterId, final ApiCallback<FilterBody> callback) {
        final String description = "getFilter userId : " + userId + " filterId : " + filterId;

        mApi.getFilterById(userId, filterId)
                .enqueue(new RestAdapterCallback<FilterBody>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getFilter(userId, filterId, callback);
                    }
                }));
    }
}
