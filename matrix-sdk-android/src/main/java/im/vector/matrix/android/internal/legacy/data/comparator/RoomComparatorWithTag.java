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

package im.vector.matrix.android.internal.legacy.data.comparator;

import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomTag;

import java.util.Comparator;

/**
 * This class is responsible for comparing rooms by the tag's order
 */
public class RoomComparatorWithTag implements Comparator<Room> {

    private final String mTag;

    public RoomComparatorWithTag(final String tag) {
        mTag = tag;
    }

    @Override
    public int compare(final Room r1, final Room r2) {
        final int res;
        final RoomTag tag1 = r1.getAccountData().roomTag(mTag);
        final RoomTag tag2 = r2.getAccountData().roomTag(mTag);

        if (tag1 != null && tag1.mOrder != null && tag2 != null && tag2.mOrder != null) {
            res = Double.compare(tag1.mOrder, tag2.mOrder);
        } else if (tag1 != null && tag1.mOrder != null) {
            res = 1;
        } else {
            res = -1;
        }
        return res;
    }
}
