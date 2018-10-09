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

import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.rest.model.Versions;

/**
 * Companion for class {@link Versions}
 */
public class VersionsUtil {

    private static final String FEATURE_LAZY_LOAD_MEMBERS = "m.lazy_load_members";

    /**
     * Return true if the server support the lazy loading of room members
     *
     * @param versions the Versions object from the server request
     * @return true if the server support the lazy loading of room members
     */
    public static boolean supportLazyLoadMembers(@Nullable Versions versions) {
        return versions != null
                && versions.unstableFeatures != null
                && versions.unstableFeatures.containsKey(FEATURE_LAZY_LOAD_MEMBERS)
                && versions.unstableFeatures.get(FEATURE_LAZY_LOAD_MEMBERS);
    }
}
