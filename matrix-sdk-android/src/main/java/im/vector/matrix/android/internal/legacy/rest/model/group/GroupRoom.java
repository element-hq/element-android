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
package im.vector.matrix.android.internal.legacy.rest.model.group;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoom;

/**
 * This class represents a room linked to a community
 */
public class GroupRoom extends PublicRoom {

    /**
     * @return the display name
     */
    public String getDisplayName() {
        if (!TextUtils.isEmpty(name)) {
            return name;
        }

        if (!TextUtils.isEmpty(canonicalAlias)) {
            return canonicalAlias;
        }

        return roomId;
    }
}
