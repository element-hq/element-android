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
 * subclass representing a search API response
 */
public class SearchEventContext {
    /**
     * Pagination token for the start of the chunk.
     */
    public String start;

    /**
     * Pagination token for the end of the chunk.
     */
    public String end;

    /**
     * Events just before the result.
     */
    public List<Event> eventsBefore;

    /**
     * Events just after the result.
     */
    public List<Event> eventsAfter;

    /**
     * The historic profile information of the users that sent the events returned.
     * The key is the user id, the value the user profile.
     */
    public Map<String, SearchUserProfile> profileInfo;
}
