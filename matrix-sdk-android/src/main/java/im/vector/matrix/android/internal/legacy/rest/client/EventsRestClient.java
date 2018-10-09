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
package im.vector.matrix.android.internal.legacy.rest.client;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.EventsApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.URLPreview;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThirdPartyProtocol;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoomsFilter;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoomsParams;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoomsResponse;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchParams;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchResponse;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchRoomEventCategoryParams;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchUsersParams;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchUsersRequestResponse;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchUsersResponse;
import im.vector.matrix.android.internal.legacy.rest.model.sync.SyncResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class used to make requests to the events API.
 */
public class EventsRestClient extends RestClient<EventsApi> {

    private static final int EVENT_STREAM_TIMEOUT_MS = 30000;

    private String mSearchEventsPatternIdentifier = null;
    private String mSearchEventsMediaNameIdentifier = null;
    private String mSearchUsersPatternIdentifier = null;

    /**
     * {@inheritDoc}
     */
    public EventsRestClient(HomeServerConnectionConfig hsConfig) {
        super(hsConfig, EventsApi.class, "", false);
    }

    protected EventsRestClient(EventsApi api) {
        mApi = api;
    }

    /**
     * Retrieves the third party server protocols
     *
     * @param callback the asynchronous callback
     */
    public void getThirdPartyServerProtocols(final ApiCallback<Map<String, ThirdPartyProtocol>> callback) {
        final String description = "getThirdPartyServerProtocols";

        mApi.thirdPartyProtocols()
                .enqueue(new RestAdapterCallback<Map<String, ThirdPartyProtocol>>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                getThirdPartyServerProtocols(callback);
                            }
                        }));
    }

    /**
     * Get the public rooms count.
     * The count can be null.
     *
     * @param callback the public rooms count callbacks
     */
    public void getPublicRoomsCount(final ApiCallback<Integer> callback) {
        getPublicRoomsCount(null, null, false, callback);
    }

    /**
     * Get the public rooms count.
     * The count can be null.
     *
     * @param server   the server url
     * @param callback the asynchronous callback
     */
    public void getPublicRoomsCount(final String server, final ApiCallback<Integer> callback) {
        getPublicRoomsCount(server, null, false, callback);
    }

    /**
     * Get the public rooms count.
     * The count can be null.
     *
     * @param server               the server url
     * @param thirdPartyInstanceId the third party instance id (optional)
     * @param includeAllNetworks   true to search in all the connected network
     * @param callback             the asynchronous callback
     */
    public void getPublicRoomsCount(final String server,
                                    final String thirdPartyInstanceId,
                                    final boolean includeAllNetworks,
                                    final ApiCallback<Integer> callback) {
        loadPublicRooms(server, thirdPartyInstanceId, includeAllNetworks, null, null, 0, new SimpleApiCallback<PublicRoomsResponse>(callback) {
            @Override
            public void onSuccess(PublicRoomsResponse publicRoomsResponse) {
                callback.onSuccess(publicRoomsResponse.total_room_count_estimate);
            }
        });
    }

    /**
     * Get the list of the public rooms.
     *
     * @param server               search on this home server only (null for any one)
     * @param thirdPartyInstanceId the third party instance id (optional)
     * @param includeAllNetworks   true to search in all the connected network
     * @param pattern              the pattern to search
     * @param since                the pagination token
     * @param limit                the maximum number of public rooms
     * @param callback             the public rooms callbacks
     */
    public void loadPublicRooms(final String server,
                                final String thirdPartyInstanceId,
                                final boolean includeAllNetworks,
                                final String pattern,
                                final String since,
                                final int limit,
                                final ApiCallback<PublicRoomsResponse> callback) {
        final String description = "loadPublicRooms";

        PublicRoomsParams publicRoomsParams = new PublicRoomsParams();

        publicRoomsParams.thirdPartyInstanceId = thirdPartyInstanceId;
        publicRoomsParams.includeAllNetworks = includeAllNetworks;
        publicRoomsParams.limit = Math.max(0, limit);
        publicRoomsParams.since = since;

        if (!TextUtils.isEmpty(pattern)) {
            publicRoomsParams.filter = new PublicRoomsFilter();
            publicRoomsParams.filter.generic_search_term = pattern;
        }

        mApi.publicRooms(server, publicRoomsParams)
                .enqueue(new RestAdapterCallback<PublicRoomsResponse>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                loadPublicRooms(server, thirdPartyInstanceId, includeAllNetworks, pattern, since, limit, callback);
                            }
                        }));
    }


    /**
     * Synchronise the client's state and receive new messages. Based on server sync C-S v2 API.
     * <p>
     * Synchronise the client's state with the latest state on the server.
     * Client's use this API when they first log in to get an initial snapshot
     * of the state on the server, and then continue to call this API to get
     * incremental deltas to the state, and to receive new messages.
     *
     * @param token            the token to stream from (nil in case of initial sync).
     * @param serverTimeout    the maximum time in ms to wait for an event.
     * @param clientTimeout    the maximum time in ms the SDK must wait for the server response.
     * @param setPresence      the optional parameter which controls whether the client is automatically
     *                         marked as online by polling this API. If this parameter is omitted then the client is
     *                         automatically marked as online when it uses this API. Otherwise if
     *                         the parameter is set to "offline" then the client is not marked as
     *                         being online when it uses this API.
     * @param filterOrFilterId a JSON filter or the ID of a filter created using the filter API (optional).
     * @param callback         The request callback
     */
    public void syncFromToken(final String token,
                              final int serverTimeout,
                              final int clientTimeout,
                              final String setPresence,
                              final String filterOrFilterId,
                              final ApiCallback<SyncResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        int timeout = (EVENT_STREAM_TIMEOUT_MS / 1000);

        if (!TextUtils.isEmpty(token)) {
            params.put("since", token);
        }

        if (-1 != serverTimeout) {
            timeout = serverTimeout;
        }

        if (!TextUtils.isEmpty(setPresence)) {
            params.put("set_presence", setPresence);
        }

        if (!TextUtils.isEmpty(filterOrFilterId)) {
            params.put("filter", filterOrFilterId);
        }

        params.put("timeout", timeout);

        // increase the timeout because the init sync might require more time to be built
        setConnectionTimeout(RestClient.CONNECTION_TIMEOUT_MS * ((null == token) ? 2 : 1));

        final String description = "syncFromToken";
        // Disable retry because it interferes with clientTimeout
        // Let the client manage retries on events streams
        mApi.sync(params)
                .enqueue(new RestAdapterCallback<SyncResponse>(description, null, false, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                syncFromToken(token, serverTimeout, clientTimeout, setPresence, filterOrFilterId, callback);
                            }
                        }));
    }

    /**
     * Retrieve an event from its event id.
     *
     * @param eventId  the event id
     * @param callback the asynchronous callback.
     */
    public void getEventFromEventId(final String eventId, final ApiCallback<Event> callback) {
        final String description = "getEventFromEventId : eventId " + eventId;

        mApi.getEvent(eventId)
                .enqueue(new RestAdapterCallback<Event>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getEventFromEventId(eventId, callback);
                    }
                }));
    }

    /**
     * Search a text in room messages.
     *
     * @param text        the text to search for.
     * @param rooms       a list of rooms to search in. nil means all rooms the user is in.
     * @param beforeLimit the number of events to get before the matching results.
     * @param afterLimit  the number of events to get after the matching results.
     * @param nextBatch   the token to pass for doing pagination from a previous response.
     * @param callback    the request callback
     */
    public void searchMessagesByText(final String text,
                                     final List<String> rooms,
                                     final int beforeLimit,
                                     final int afterLimit,
                                     final String nextBatch,
                                     final ApiCallback<SearchResponse> callback) {
        SearchParams searchParams = new SearchParams();
        SearchRoomEventCategoryParams searchEventParams = new SearchRoomEventCategoryParams();

        searchEventParams.search_term = text;
        searchEventParams.order_by = "recent";

        searchEventParams.event_context = new HashMap<>();
        searchEventParams.event_context.put("before_limit", beforeLimit);
        searchEventParams.event_context.put("after_limit", afterLimit);
        searchEventParams.event_context.put("include_profile", true);

        if (null != rooms) {
            searchEventParams.filter = new HashMap<>();
            searchEventParams.filter.put("rooms", rooms);
        }

        searchParams.search_categories = new HashMap<>();
        searchParams.search_categories.put("room_events", searchEventParams);

        final String description = "searchMessageText";

        final String uid = System.currentTimeMillis() + "";
        mSearchEventsPatternIdentifier = uid + text;

        // don't retry to send the request
        // if the search fails, stop it
        mApi.searchEvents(searchParams, nextBatch)
                .enqueue(new RestAdapterCallback<SearchResponse>(description, null, new ApiCallback<SearchResponse>() {
                    /**
                     * Tells if the current response for the latest request.
                     *
                     * @return true if it is the response of the latest request.
                     */
                    private boolean isActiveRequest() {
                        return TextUtils.equals(mSearchEventsPatternIdentifier, uid + text);
                    }

                    @Override
                    public void onSuccess(SearchResponse response) {
                        if (isActiveRequest()) {
                            if (null != callback) {
                                callback.onSuccess(response);
                            }

                            mSearchEventsPatternIdentifier = null;
                        }
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        if (isActiveRequest()) {
                            if (null != callback) {
                                callback.onNetworkError(e);
                            }

                            mSearchEventsPatternIdentifier = null;
                        }
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        if (isActiveRequest()) {
                            if (null != callback) {
                                callback.onMatrixError(e);
                            }

                            mSearchEventsPatternIdentifier = null;
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        if (isActiveRequest()) {
                            if (null != callback) {
                                callback.onUnexpectedError(e);
                            }

                            mSearchEventsPatternIdentifier = null;
                        }
                    }

                }, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        searchMessagesByText(text, rooms, beforeLimit, afterLimit, nextBatch, callback);
                    }
                }));
    }

    /**
     * Search a media from its name.
     *
     * @param name        the text to search for.
     * @param rooms       a list of rooms to search in. nil means all rooms the user is in.
     * @param beforeLimit the number of events to get before the matching results.
     * @param afterLimit  the number of events to get after the matching results.
     * @param nextBatch   the token to pass for doing pagination from a previous response.
     * @param callback    the request callback
     */
    public void searchMediasByText(final String name,
                                   final List<String> rooms,
                                   final int beforeLimit,
                                   final int afterLimit,
                                   final String nextBatch,
                                   final ApiCallback<SearchResponse> callback) {
        SearchParams searchParams = new SearchParams();
        SearchRoomEventCategoryParams searchEventParams = new SearchRoomEventCategoryParams();

        searchEventParams.search_term = name;
        searchEventParams.order_by = "recent";

        searchEventParams.event_context = new HashMap<>();
        searchEventParams.event_context.put("before_limit", beforeLimit);
        searchEventParams.event_context.put("after_limit", afterLimit);
        searchEventParams.event_context.put("include_profile", true);

        searchEventParams.filter = new HashMap<>();

        if (null != rooms) {
            searchEventParams.filter.put("rooms", rooms);
        }

        List<String> types = new ArrayList<>();
        types.add(Event.EVENT_TYPE_MESSAGE);
        searchEventParams.filter.put("types", types);

        searchEventParams.filter.put("contains_url", true);

        searchParams.search_categories = new HashMap<>();
        searchParams.search_categories.put("room_events", searchEventParams);

        // other unused filter items
        // not_types
        // not_rooms
        // senders
        // not_senders

        final String uid = System.currentTimeMillis() + "";
        mSearchEventsMediaNameIdentifier = uid + name;

        final String description = "searchMediasByText";

        // don't retry to send the request
        // if the search fails, stop it
        mApi.searchEvents(searchParams, nextBatch)
                .enqueue(new RestAdapterCallback<SearchResponse>(description, null, new ApiCallback<SearchResponse>() {
                    /**
                     * Tells if the current response for the latest request.
                     *
                     * @return true if it is the response of the latest request.
                     */
                    private boolean isActiveRequest() {
                        return TextUtils.equals(mSearchEventsMediaNameIdentifier, uid + name);
                    }

                    @Override
                    public void onSuccess(SearchResponse newSearchResponse) {
                        if (isActiveRequest()) {
                            callback.onSuccess(newSearchResponse);
                            mSearchEventsMediaNameIdentifier = null;
                        }
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        if (isActiveRequest()) {
                            callback.onNetworkError(e);
                            mSearchEventsMediaNameIdentifier = null;
                        }
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        if (isActiveRequest()) {
                            callback.onMatrixError(e);
                            mSearchEventsMediaNameIdentifier = null;
                        }
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        if (isActiveRequest()) {
                            callback.onUnexpectedError(e);
                            mSearchEventsMediaNameIdentifier = null;
                        }
                    }

                }, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        searchMediasByText(name, rooms, beforeLimit, afterLimit, nextBatch, callback);
                    }
                }));
    }


    /**
     * Search users with a patter,
     *
     * @param text          the text to search for.
     * @param limit         the maximum nbr of users in the response
     * @param userIdsFilter the userIds to exclude from the result
     * @param callback      the request callback
     */
    public void searchUsers(final String text, final Integer limit, final Set<String> userIdsFilter, final ApiCallback<SearchUsersResponse> callback) {
        SearchUsersParams searchParams = new SearchUsersParams();

        searchParams.search_term = text;
        searchParams.limit = limit + ((null != userIdsFilter) ? userIdsFilter.size() : 0);

        final String uid = mSearchUsersPatternIdentifier = System.currentTimeMillis() + " " + text + " " + limit;
        final String description = "searchUsers";

        // don't retry to send the request
        // if the search fails, stop it
        mApi.searchUsers(searchParams)
                .enqueue(new RestAdapterCallback<SearchUsersRequestResponse>(description, null,
                        new ApiCallback<SearchUsersRequestResponse>() {
                            /**
                             * Tells if the current response for the latest request.
                             *
                             * @return true if it is the response of the latest request.
                             */
                            private boolean isActiveRequest() {
                                return TextUtils.equals(mSearchUsersPatternIdentifier, uid);
                            }

                            @Override
                            public void onSuccess(SearchUsersRequestResponse aResponse) {
                                if (isActiveRequest()) {
                                    SearchUsersResponse response = new SearchUsersResponse();
                                    response.limited = aResponse.limited;
                                    response.results = new ArrayList<>();
                                    Set<String> filter = (null != userIdsFilter) ? userIdsFilter : new HashSet<String>();

                                    if (null != aResponse.results) {
                                        for (SearchUsersRequestResponse.User user : aResponse.results) {
                                            if ((null != user.user_id) && !filter.contains(user.user_id)) {
                                                User addedUser = new User();
                                                addedUser.user_id = user.user_id;
                                                addedUser.avatar_url = user.avatar_url;
                                                addedUser.displayname = user.display_name;
                                                response.results.add(addedUser);
                                            }
                                        }
                                    }

                                    callback.onSuccess(response);
                                    mSearchUsersPatternIdentifier = null;
                                }
                            }

                            @Override
                            public void onNetworkError(Exception e) {
                                if (isActiveRequest()) {
                                    callback.onNetworkError(e);
                                    mSearchUsersPatternIdentifier = null;
                                }
                            }

                            @Override
                            public void onMatrixError(MatrixError e) {
                                if (isActiveRequest()) {
                                    callback.onMatrixError(e);
                                    mSearchUsersPatternIdentifier = null;
                                }
                            }

                            @Override
                            public void onUnexpectedError(Exception e) {
                                if (isActiveRequest()) {
                                    callback.onUnexpectedError(e);
                                    mSearchUsersPatternIdentifier = null;
                                }
                            }

                        }, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        searchUsers(text, limit, userIdsFilter, callback);
                    }
                }));
    }

    /**
     * Cancel any pending file search request
     */
    public void cancelSearchMediasByText() {
        mSearchEventsMediaNameIdentifier = null;
    }

    /**
     * Cancel any pending search request
     */
    public void cancelSearchMessagesByText() {
        mSearchEventsPatternIdentifier = null;
    }

    /**
     * Cancel any pending search request
     */
    public void cancelUsersSearch() {
        mSearchUsersPatternIdentifier = null;
    }

    /**
     * Retrieve the URL preview information.
     *
     * @param url      the URL
     * @param ts       the timestamp
     * @param callback the asynchronous callback
     */
    public void getURLPreview(final String url, final long ts, final ApiCallback<URLPreview> callback) {
        final String description = "getURLPreview : URL " + url + " with ts " + ts;

        mApi.getURLPreview(url, ts)
                .enqueue(new RestAdapterCallback<Map<String, Object>>(description, null, false,
                        new SimpleApiCallback<Map<String, Object>>(callback) {
                            @Override
                            public void onSuccess(Map<String, Object> map) {
                                if (null != callback) {
                                    callback.onSuccess(new URLPreview(map, url));
                                }
                            }
                        }, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getURLPreview(url, ts, callback);
                    }
                }));
    }
}
