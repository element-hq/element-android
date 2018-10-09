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

import com.google.gson.JsonElement;

/**
 * Contains optional extra information about the event.
 */
public class UnsignedData implements java.io.Serializable {

    /**
     * The time in milliseconds that has elapsed since the event was sent
     */
    public Long age;

    /**
     * The reason this event was redacted, if it was redacted
     */
    public RedactedBecause redacted_because;

    /**
     * The client-supplied transaction ID, if the client being given the event is the same one which sent it.
     */
    public String transaction_id;

    /**
     * The previous event content (room member information only)
     */
    public transient JsonElement prev_content;
}
