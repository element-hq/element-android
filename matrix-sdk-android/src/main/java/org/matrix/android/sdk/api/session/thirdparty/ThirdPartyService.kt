/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C
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

package org.matrix.android.sdk.api.session.thirdparty

import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser

/**
 * See https://matrix.org/docs/spec/client_server/r0.4.0.html#get-matrix-client-r0-thirdparty-protocols
 */
interface ThirdPartyService {

    /**
     * Fetches the overall metadata about protocols supported by the homeserver.
     * Includes both the available protocols and all fields required for queries against each protocol.
     */
    suspend fun getThirdPartyProtocols(): Map<String, ThirdPartyProtocol>

    /**
     * Retrieve a Matrix User ID linked to a user on the third party service, given a set of user parameters.
     * @param protocol Required. The name of the protocol.
     * @param fields One or more custom fields that are passed to the AS to help identify the user.
     */
    suspend fun getThirdPartyUser(protocol: String, fields: Map<String, String> = emptyMap()): List<ThirdPartyUser>
}
