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

import java.io.Serializable;

/**
 * This class represents the current user status in a group summary response.
 */
public class GroupSummaryUser implements Serializable {

    /**
     * The current user membership in this community.
     */
    public String membership;

    /**
     * Tell whether the user published this community on his profile.
     */
    public Boolean isPublicised;
}
