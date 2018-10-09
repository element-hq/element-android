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

import java.util.List;

/**
 * Class representing the forward room key request body content
 */
public class ForwardedRoomKeyContent implements java.io.Serializable {
    public String algorithm;

    public String room_id;

    public String sender_key;

    public String session_id;

    public String session_key;

    public List<String> forwarding_curve25519_key_chain;

    public String sender_claimed_ed25519_key;
}