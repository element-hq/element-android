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
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.rest.model.Event;

/**
 * Useful methods to deals with Matrix permalink
 */
public class PermalinkUtils {

    private static final String MATRIX_TO_URL_BASE = "https://matrix.to/#/";

    /**
     * Creates a permalink for an event.
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param event the event
     * @return the permalink, or null in case of error
     */
    @Nullable
    public static String createPermalink(Event event) {
        if (event == null) {
            return null;
        }

        return createPermalink(event.roomId, event.eventId);
    }

    /**
     * Creates a permalink for an id (can be a user Id, Room Id, etc.).
     * Ex: "https://matrix.to/#/@benoit:matrix.org"
     *
     * @param id the id
     * @return the permalink, or null in case of error
     */
    @Nullable
    public static String createPermalink(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }

        return MATRIX_TO_URL_BASE + escape(id);
    }

    /**
     * Creates a permalink for an event. If you have an event you can use {@link #createPermalink(Event)}
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param roomId  the id of the room
     * @param eventId the id of the event
     * @return the permalink
     */
    @NonNull
    public static String createPermalink(@NonNull String roomId, @NonNull String eventId) {
        return MATRIX_TO_URL_BASE + escape(roomId) + "/" + escape(eventId);
    }

    /**
     * Extract the linked id from the universal link
     *
     * @param url the universal link, Ex: "https://matrix.to/#/@benoit:matrix.org"
     * @return the id from the url, ex: "@benoit:matrix.org", or null if the url is not a permalink
     */
    public static String getLinkedId(@Nullable String url) {
        boolean isSupported = url != null && url.startsWith(MATRIX_TO_URL_BASE);

        if (isSupported) {
            return url.substring(MATRIX_TO_URL_BASE.length());
        }

        return null;
    }


    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private static String escape(String id) {
        return id.replaceAll("/", "%2F");
    }
}
