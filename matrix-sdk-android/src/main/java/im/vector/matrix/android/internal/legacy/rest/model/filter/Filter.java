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

import java.util.List;

/**
 * Represents "Filter" as mentioned in the SPEC
 * https://matrix.org/docs/spec/client_server/r0.3.0.html#post-matrix-client-r0-user-userid-filter
 */
public class Filter {

    public Integer limit;

    public List<String> senders;

    @SerializedName("not_senders")
    public List<String> notSenders;

    public List<String> types;

    @SerializedName("not_types")
    public List<String> notTypes;

    public List<String> rooms;

    @SerializedName("not_rooms")
    public List<String> notRooms;

    public boolean hasData() {
        return limit != null
                || senders != null
                || notSenders != null
                || types != null
                || notTypes != null
                || rooms != null
                || notRooms != null;
    }
}
