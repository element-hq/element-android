/*
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
package im.vector.matrix.android.internal.legacy.rest.model;


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Content of a m.room.create type event
 */
public class RoomCreateContent implements Serializable {

    public String creator;
    public Predecessor predecessor;

    public RoomCreateContent deepCopy() {
        final RoomCreateContent copy = new RoomCreateContent();
        copy.creator = creator;
        copy.predecessor = predecessor != null ? predecessor.deepCopy() : null;
        return copy;
    }

    public boolean hasPredecessor() {
        return predecessor != null;
    }

    /**
     * A link to an old room in case of room versioning
     */
    public static class Predecessor implements Serializable {

        @SerializedName("room_id")
        public String roomId;

        @SerializedName("event_id")
        public String eventId;

        public Predecessor deepCopy() {
            final Predecessor copy = new Predecessor();
            copy.roomId = roomId;
            copy.eventId = eventId;
            return copy;
        }
    }


}
