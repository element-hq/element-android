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

package im.vector.matrix.android.internal.legacy.data;

import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.rest.model.Event;

import java.util.Map;
import java.util.Set;

/**
 * Class representing private data that the user has defined for a room.
 */
public class RoomAccountData implements java.io.Serializable {

    private static final long serialVersionUID = -8406116277864521120L;

    // The tags the user defined for this room.
    // The key is the tag name. The value, the associated MXRoomTag object.
    private Map<String, RoomTag> tags = null;

    /**
     * Process an event that modifies room account data (like m.tag event).
     *
     * @param event an event
     */
    public void handleTagEvent(Event event) {
        if (event.getType().equals(Event.EVENT_TYPE_TAGS)) {
            tags = RoomTag.roomTagsWithTagEvent(event);
        }
    }

    /**
     * Provide a RoomTag for a key.
     *
     * @param key the key.
     * @return the roomTag if it is found else null
     */
    @Nullable
    public RoomTag roomTag(String key) {
        if ((null != tags) && tags.containsKey(key)) {
            return tags.get(key);
        }

        return null;
    }

    /**
     * @return true if some tags are defined
     */
    public boolean hasTags() {
        return (null != tags) && (tags.size() > 0);
    }

    /**
     * @return the list of keys, or null if no tag
     */
    @Nullable
    public Set<String> getKeys() {
        if (hasTags()) {
            return tags.keySet();
        } else {
            return null;
        }
    }
}
