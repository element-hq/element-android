/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the group users in the server response.
 */
public class GroupUsers implements Serializable {

    public Integer totalUserCountEstimate;

    public List<GroupUser> chunk;

    // the server sends some duplicated entries
    private List<GroupUser> mFilteredUsers;

    /**
     * @return the users list
     */
    public List<GroupUser> getUsers() {
        if (null == chunk) {
            mFilteredUsers = chunk = new ArrayList<>();
        } else if (null == mFilteredUsers) {
            mFilteredUsers = new ArrayList<>();

            Map<String, GroupUser> map = new HashMap<>();

            for (GroupUser user : chunk) {
                if (null != user.userId) {
                    map.put(user.userId, user);
                } else {
                    mFilteredUsers.add(user);
                }
            }
            mFilteredUsers.addAll(map.values());
        }

        return mFilteredUsers;
    }

    /**
     * @return the estimated users count
     */
    public int getEstimatedUsersCount() {
        if (null == totalUserCountEstimate) {
            totalUserCountEstimate = getUsers().size();
        }

        return totalUserCountEstimate;
    }
}
