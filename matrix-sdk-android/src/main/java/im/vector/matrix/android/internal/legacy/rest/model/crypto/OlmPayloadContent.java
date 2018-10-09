/*
 * Copyright 2016 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.crypto;

import java.util.Map;

/**
 * Class representing the OLM payload content
 */
public class OlmPayloadContent implements java.io.Serializable {
    /**
     * The room id
     */
    public String room_id;

    /**
     * The sender
     */
    public String sender;

    /**
     * The receipient
     */
    public String recipient;

    /**
     * the recipient keys
     */
    public Map<String, String> recipient_keys;

    /**
     * The keys
     */
    public Map<String, String> keys;
}