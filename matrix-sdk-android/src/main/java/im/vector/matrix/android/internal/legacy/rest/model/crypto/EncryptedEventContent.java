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

/**
 * Class representing an encrypted event content
 */
public class EncryptedEventContent implements java.io.Serializable {

    /**
     * the used algorithm
     */
    public String algorithm;

    /**
     * The encrypted event
     */
    public String ciphertext;

    /**
     * The device id
     */
    public String device_id;

    /**
     * the sender key
     */
    public String sender_key;

    /**
     * The session id
     */
    public String session_id;
}