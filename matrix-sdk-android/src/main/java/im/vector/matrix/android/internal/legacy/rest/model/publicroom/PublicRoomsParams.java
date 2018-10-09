/*
 * Copyright 2016 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.publicroom;

/**
 * Class to pass parameters to get the public rooms list
 */
public class PublicRoomsParams {
    /**
     * The third party instance id
     */
    public String thirdPartyInstanceId;

    /**
     * Tell if the query must be done in all the connected networks.
     */
    public boolean includeAllNetworks;

    /**
     * Maximum number of entries to return
     **/
    public Integer limit;

    /**
     * token to paginate from
     **/
    public String since;

    /**
     * Filter parameters
     **/
    public PublicRoomsFilter filter;
}
