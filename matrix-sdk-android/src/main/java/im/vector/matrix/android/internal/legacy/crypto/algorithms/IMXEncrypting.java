/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.crypto.algorithms;

import com.google.gson.JsonElement;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;

import java.util.List;

/**
 * An interface for encrypting data
 */
public interface IMXEncrypting {

    /**
     * Init
     *
     * @param matrixSession the related 'MXSession'.
     * @param roomId        the id of the room we will be sending to.
     */
    void initWithMatrixSession(MXSession matrixSession, String roomId);

    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType    the type of the event.
     * @param userIds      the room members the event will be sent to.
     * @param callback     the asynchronous callback
     */
    void encryptEventContent(JsonElement eventContent, String eventType, List<String> userIds, ApiCallback<JsonElement> callback);
}
