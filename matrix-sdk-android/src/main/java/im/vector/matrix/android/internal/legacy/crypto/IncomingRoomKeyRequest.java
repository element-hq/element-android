/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.crypto;

import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyRequest;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyRequestBody;

import im.vector.matrix.android.internal.legacy.util.JsonUtils;

import java.io.Serializable;

/**
 * IncomingRoomKeyRequest class defines the incoming room keys request.
 */
public class IncomingRoomKeyRequest implements Serializable {
    /**
     * The user id
     */
    public String mUserId;

    /**
     * The device id
     */
    public String mDeviceId;

    /**
     * The request id
     */
    public String mRequestId;

    /**
     * The request body
     */
    public RoomKeyRequestBody mRequestBody;

    /**
     * The runnable to call to accept to share the keys
     */
    public transient Runnable mShare;

    /**
     * The runnable to call to ignore the key share request.
     */
    public transient Runnable mIgnore;

    /**
     * Constructor
     *
     * @param event the event
     */
    public IncomingRoomKeyRequest(Event event) {
        mUserId = event.getSender();

        RoomKeyRequest roomKeyRequest = JsonUtils.toRoomKeyRequest(event.getContentAsJsonObject());
        mDeviceId = roomKeyRequest.requesting_device_id;
        mRequestId = roomKeyRequest.request_id;
        mRequestBody = (null != roomKeyRequest.body) ? roomKeyRequest.body : new RoomKeyRequestBody();
    }
}

