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
package im.vector.matrix.android.internal.legacy.rest.model;

/**
 * subclass representing a search API response
 */
public class Signed implements java.io.Serializable {
    /**
     * The token property of the containing third_party_invite object.
     */
    public String token;

    /**
     * A single signature from the verifying server, in the format specified by the Signing Events section of the server-server API.
     */
    public Object signatures;

    /**
     * The invited matrix user ID. Must be equal to the user_id property of the event.
     */
    public String mxid;
}