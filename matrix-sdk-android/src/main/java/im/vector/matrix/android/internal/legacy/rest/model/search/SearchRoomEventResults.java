/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.rest.model.search;

import im.vector.matrix.android.internal.legacy.rest.model.Event;

import java.util.List;
import java.util.Map;

/**
 * Class representing a search API response
 */
public class SearchRoomEventResults {
    /**
     * Total number of results found.
     */
    public Integer count;

    /**
     * List of results in the requested order.
     */
    public List<SearchResult> results;

    /**
     * The current state for every room in the results.
     * This is included if the request had the include_state key set with a value of true.
     * The key is the roomId, the value its state. (TODO_SEARCH: right?)
     */
    public Map<String, List<Event>> state;

    /**
     * Any groups that were requested.
     * The key is the group id (TODO_SEARCH).
     */
    public Map<String, SearchGroup> groups;

    /**
     * Token that can be used to get the next batch of results in the group, if exists.
     */
    public String nextBatch;
}
