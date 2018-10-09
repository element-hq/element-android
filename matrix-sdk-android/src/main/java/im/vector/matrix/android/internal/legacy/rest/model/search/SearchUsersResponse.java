/*
 * Copyright 2017 Vector Creations Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.search;

import im.vector.matrix.android.internal.legacy.rest.model.User;

import java.util.List;

/**
 * Class representing an users search response
 */
public class SearchUsersResponse {

    // indicates if the result list has been truncated by the limit.
    public Boolean limited;

    // set a limit to the request response
    public List<User> results;
}
