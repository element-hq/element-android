/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.conference.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import javax.inject.Inject

class JitsiJWTFactory @Inject constructor() {

    /**
     * Create a JWT token for jitsi openidtoken-jwt authentication
     * See https://github.com/matrix-org/prosody-mod-auth-matrix-user-verification
     */
    fun create(jitsiServerDomain: String,
              openIdAccessToken: String,
              roomId: String,
              userAvatarUrl: String,
              userDisplayName: String): String {

        // The secret key here is irrelevant, we're only using the JWT to transport data to Prosody in the Jitsi stack.
        val key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        val context = mapOf(
                "user" to mapOf(
                        "name" to userDisplayName,
                        "avatar" to userAvatarUrl
                ),
                "matrix" to mapOf(
                        "token" to openIdAccessToken,
                        "room_id" to roomId
                )
        )
        return Jwts.builder()
                .setIssuer(jitsiServerDomain)
                .setSubject(jitsiServerDomain)
                .setAudience("https://$jitsiServerDomain")
                // room is not used at the moment, a * works here.
                .claim("room", "*")
                .claim("context", context)
                .signWith(key)
                .compact()
    }
}
