/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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
package im.vector.matrix.android.internal.legacy.rest.api;

import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThirdPartyProtocol;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoomsParams;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoomsResponse;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchParams;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchResponse;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchUsersParams;
import im.vector.matrix.android.internal.legacy.rest.model.search.SearchUsersRequestResponse;
import im.vector.matrix.android.internal.legacy.rest.model.sync.SyncResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * The events API.
 */
public interface EventsApi {

    /**
     * Perform the initial sync to find the rooms that concern the user, the participants' presence, etc.
     *
     * @param params the GET params.
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "sync")
    Call<SyncResponse> sync(@QueryMap Map<String, Object> params);

    /**
     * Retrieve an event from its event id
     *
     * @param eventId the event Id
     */
    @GET(RestClient.URI_API_PREFIX_PATH_R0 + "events/{eventId}")
    Call<Event> getEvent(@Path("eventId") String eventId);

    /**
     * Get the third party server protocols.
     */
    @GET(RestClient.URI_API_PREFIX_PATH_UNSTABLE + "thirdparty/protocols")
    Call<Map<String, ThirdPartyProtocol>> thirdPartyProtocols();

    /**
     * Get the list of public rooms.
     *
     * @param server            the server (might be null)
     * @param publicRoomsParams the request params
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "publicRooms")
    Call<PublicRoomsResponse> publicRooms(@Query("server") String server, @Body PublicRoomsParams publicRoomsParams);

    /**
     * Perform a search.
     *
     * @param searchParams the search params.
     * @param nextBatch    the next batch token
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "search")
    Call<SearchResponse> searchEvents(@Body SearchParams searchParams, @Query("next_batch") String nextBatch);

    /**
     * Perform an users search.
     *
     * @param searchUsersParams the search params.
     */
    @POST(RestClient.URI_API_PREFIX_PATH_R0 + "user_directory/search")
    Call<SearchUsersRequestResponse> searchUsers(@Body SearchUsersParams searchUsersParams);

    /**
     * Retrieve the preview information of an URL.
     *
     * @param url the URL
     * @param ts  the ts
     */
    @GET(RestClient.URI_API_PREFIX_PATH_MEDIA_R0 + "preview_url")
    Call<Map<String, Object>> getURLPreview(@Query("url") String url, @Query("ts") long ts);
}
