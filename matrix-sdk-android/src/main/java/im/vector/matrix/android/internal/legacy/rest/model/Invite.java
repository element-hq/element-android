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
public class Invite implements java.io.Serializable {
    /**
     * A name which can be displayed to represent the user instead of their third party identifier.
     */
    public String display_name;

    /**
     * A block of content which has been signed, which servers can use to verify the event. Clients should ignore this.
     */
    public Signed signed;
}