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

package im.vector.matrix.android.internal.legacy.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.rest.model.filter.Filter;
import im.vector.matrix.android.internal.legacy.rest.model.filter.FilterBody;
import im.vector.matrix.android.internal.legacy.rest.model.filter.RoomEventFilter;
import im.vector.matrix.android.internal.legacy.rest.model.filter.RoomFilter;

import java.util.ArrayList;

public class FilterUtil {

    /**
     * Patch the filterBody to enable or disable the data save mode
     * <p>
     * If data save mode is on, FilterBody will contains
     * "{\"room\": {\"ephemeral\": {\"types\": [\"m.receipt\"]}}, \"presence\":{\"notTypes\": [\"*\"]}}"
     *
     * @param filterBody      filterBody to patch
     * @param useDataSaveMode true to enable data save mode
     */
    public static void enableDataSaveMode(@NonNull FilterBody filterBody, boolean useDataSaveMode) {
        if (useDataSaveMode) {
            // Enable data save mode
            if (filterBody.room == null) {
                filterBody.room = new RoomFilter();
            }
            if (filterBody.room.ephemeral == null) {
                filterBody.room.ephemeral = new RoomEventFilter();
            }
            if (filterBody.room.ephemeral.types == null) {
                filterBody.room.ephemeral.types = new ArrayList<>();
            }
            if (!filterBody.room.ephemeral.types.contains("m.receipt")) {
                filterBody.room.ephemeral.types.add("m.receipt");
            }

            if (filterBody.presence == null) {
                filterBody.presence = new Filter();
            }
            if (filterBody.presence.notTypes == null) {
                filterBody.presence.notTypes = new ArrayList<>();
            }
            if (!filterBody.presence.notTypes.contains("*")) {
                filterBody.presence.notTypes.add("*");
            }
        } else {
            if (filterBody.room != null
                    && filterBody.room.ephemeral != null
                    && filterBody.room.ephemeral.types != null) {
                filterBody.room.ephemeral.types.remove("m.receipt");

                if (filterBody.room.ephemeral.types.isEmpty()) {
                    filterBody.room.ephemeral.types = null;
                }

                if (!filterBody.room.ephemeral.hasData()) {
                    filterBody.room.ephemeral = null;
                }

                if (!filterBody.room.hasData()) {
                    filterBody.room = null;
                }
            }

            if (filterBody.presence != null
                    && filterBody.presence.notTypes != null) {
                filterBody.presence.notTypes.remove("*");

                if (filterBody.presence.notTypes.isEmpty()) {
                    filterBody.presence.notTypes = null;
                }

                if (!filterBody.presence.hasData()) {
                    filterBody.presence = null;
                }
            }
        }
    }

    /**
     * Patch the filterBody to enable or disable the lazy loading
     * <p>
     * If lazy loading is on, the filterBody will looks like
     * {"room":{"state":{"lazy_load_members":true})}
     *
     * @param filterBody     filterBody to patch
     * @param useLazyLoading true to enable lazy loading
     */
    public static void enableLazyLoading(FilterBody filterBody, boolean useLazyLoading) {
        if (useLazyLoading) {
            // Enable lazy loading
            if (filterBody.room == null) {
                filterBody.room = new RoomFilter();
            }
            if (filterBody.room.state == null) {
                filterBody.room.state = new RoomEventFilter();
            }

            filterBody.room.state.lazyLoadMembers = true;
        } else {
            if (filterBody.room != null
                    && filterBody.room.state != null) {
                filterBody.room.state.lazyLoadMembers = null;

                if (!filterBody.room.state.hasData()) {
                    filterBody.room.state = null;
                }

                if (!filterBody.room.hasData()) {
                    filterBody.room = null;
                }
            }
        }
    }

    /**
     * Create a RoomEventFilter
     *
     * @param withLazyLoading true when lazy loading is enabled
     * @return a RoomEventFilter or null if lazy loading if OFF
     */
    @Nullable
    public static RoomEventFilter createRoomEventFilter(boolean withLazyLoading) {
        RoomEventFilter roomEventFilter = null;

        if (withLazyLoading) {
            roomEventFilter = new RoomEventFilter();
            roomEventFilter.lazyLoadMembers = true;
        }

        return roomEventFilter;
    }
}
