/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.uia

import org.matrix.android.sdk.api.auth.UIABaseAuth

data class DefaultBaseAuth(
        /**
         * This is a session identifier that the client must pass back to the homeserver,
         * if one is provided, in subsequent attempts to authenticate in the same API call.
         */
        override val session: String? = null

) : UIABaseAuth {
    override fun hasAuthInfo() = true

    override fun copyWithSession(session: String) = this.copy(session = session)

    override fun asMap(): Map<String, *> = mapOf("session" to session)
}
