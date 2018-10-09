/*
 * Copyright 2015 OpenMarket Ltd
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

import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.RoomTags;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Class representing a room tag.
 */
public class RoomTag implements java.io.Serializable {
    private static final long serialVersionUID = 5172602958896551204L;
    private static final String LOG_TAG = RoomTag.class.getSimpleName();

    //
    public static final String ROOM_TAG_FAVOURITE = "m.favourite";
    public static final String ROOM_TAG_LOW_PRIORITY = "m.lowpriority";
    public static final String ROOM_TAG_NO_TAG = "m.recent";
    public static final String ROOM_TAG_SERVER_NOTICE = "m.server_notice";

    /**
     * The name of a tag.
     */
    public String mName;

    /**
     * Try to parse order as Double.
     * Provides nil if the items cannot be parsed.
     */
    public Double mOrder;

    /**
     * RoomTag creator.
     *
     * @param aName   the tag name.
     * @param anOrder the tag order
     */
    public RoomTag(String aName, Double anOrder) {
        mName = aName;
        mOrder = anOrder;
    }

    /**
     * Extract a list of tags from a room tag event.
     *
     * @param event a room tag event (which can contains several tags)
     * @return a dictionary containing the tags the user defined for one room.
     */
    public static Map<String, RoomTag> roomTagsWithTagEvent(Event event) {
        Map<String, RoomTag> tags = new HashMap<>();

        try {
            RoomTags roomtags = JsonUtils.toRoomTags(event.getContent());

            if ((null != roomtags.tags) && (0 != roomtags.tags.size())) {
                for (String tagName : roomtags.tags.keySet()) {
                    Map<String, Double> params = roomtags.tags.get(tagName);
                    if (params != null) {
                        tags.put(tagName, new RoomTag(tagName, params.get("order")));
                    } else {
                        tags.put(tagName, new RoomTag(tagName, null));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "roomTagsWithTagEvent fails " + e.getMessage(), e);
        }

        return tags;
    }
}
