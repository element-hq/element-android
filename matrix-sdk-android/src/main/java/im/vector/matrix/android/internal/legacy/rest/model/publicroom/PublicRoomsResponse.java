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
package im.vector.matrix.android.internal.legacy.rest.model.publicroom;

import java.util.List;

/**
 * Class representing the public rooms request response
 */
public class PublicRoomsResponse {
    /**
     * token to forward paginate
     **/
    public String next_batch;

    /**
     * token to back paginate
     **/
    public String prev_batch;

    /**
     * public rooms list
     **/
    public List<PublicRoom> chunk;

    /**
     * number of unfiltered existing rooms
     **/
    public Integer total_room_count_estimate;
}
