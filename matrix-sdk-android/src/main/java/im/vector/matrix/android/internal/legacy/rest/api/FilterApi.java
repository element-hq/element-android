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
package im.vector.matrix.android.internal.legacy.rest.api;

import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterBody;
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FilterApi {

    /**
     * Upload FilterBody to get a filter_id which can be used for /sync requests
     *
     * @param userId   the user id
     * @param body   the Json representation of a FilterBody object
     */
    @POST("user/{userId}/filter")
    Call<FilterResponse> uploadFilter(@Path("userId") String userId, @Body FilterBody body);

    /**
     * Gets a filter with a given filterId from the homeserver
     *
     * @param userId   the user id
     * @param filterId the filterID
     * @return Filter
     */
    @GET("user/{userId}/filter/{filterId}")
    Call<FilterBody> getFilterById(@Path("userId") String userId, @Path("filterId") String filterId);
}
