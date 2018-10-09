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

import im.vector.matrix.android.internal.legacy.interfaces.DatedObject;

import java.util.Comparator;

public class Comparators {

    // comparator to sort from the oldest to the latest.
    public static final Comparator<DatedObject> ascComparator = new Comparator<DatedObject>() {
        @Override
        public int compare(DatedObject datedObject1, DatedObject datedObject2) {
            return (int) (datedObject1.getDate() - datedObject2.getDate());
        }
    };

    // comparator to sort from the latest to the oldest.
    public static final Comparator<DatedObject> descComparator = new Comparator<DatedObject>() {
        @Override
        public int compare(DatedObject datedObject1, DatedObject datedObject2) {
            return (int) (datedObject2.getDate() - datedObject1.getDate());
        }
    };
}
