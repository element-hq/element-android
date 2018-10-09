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
package im.vector.matrix.android.internal.legacy.rest.model.filter;

import com.google.gson.annotations.SerializedName;

import im.vector.matrix.android.internal.legacy.util.JsonUtils;

import java.util.List;

/**
 * Represents "RoomEventFilter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
public class RoomEventFilter {

    public Integer limit;

    @SerializedName("not_senders")
    public List<String> notSenders;

    @SerializedName("not_types")
    public List<String> notTypes;

    public List<String> senders;

    public List<String> types;

    public List<String> rooms;

    @SerializedName("not_rooms")
    public List<String> notRooms;

    @SerializedName("contains_url")
    public Boolean containsUrl;

    @SerializedName("lazy_load_members")
    public Boolean lazyLoadMembers;

    public boolean hasData() {
        return limit != null
                || notSenders != null
                || notTypes != null
                || senders != null
                || types != null
                || rooms != null
                || notRooms != null
                || containsUrl != null
                || lazyLoadMembers != null;
    }

    public String toJSONString() {
        return JsonUtils.getGson(false).toJson(this);
    }
}
