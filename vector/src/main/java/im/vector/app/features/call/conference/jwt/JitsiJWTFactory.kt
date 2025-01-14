/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference.jwt

import im.vector.app.core.utils.ensureProtocol
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.matrix.android.sdk.api.session.openid.OpenIdToken
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class JitsiJWTFactory @Inject constructor() {

    /**
     * Create a JWT token for jitsi openidtoken-jwt authentication
     * See https://github.com/matrix-org/prosody-mod-auth-matrix-user-verification
     */
    fun create(
            openIdToken: OpenIdToken,
            jitsiServerDomain: String,
            roomId: String,
            userAvatarUrl: String,
            userDisplayName: String
    ): String {
        // The secret key here is irrelevant, we're only using the JWT to transport data to Prosody in the Jitsi stack.
        // In the PR https://github.com/jitsi/luajwtjitsi/pull/3 the function `luajwtjitsi.decode` was removed and
        // we cannot use random secret keys anymore. But the JWT library `jjwt` doesn't accept the hardcoded key `notused`
        // from the module `prosody-mod-auth-matrix-user-verification` since it's too short and thus insecure. So, we
        // create a new token using a random key and then re-sign the token manually with the 'weak' key.
        val signatureAlgorithm = SignatureAlgorithm.HS256
        val key = Keys.secretKeyFor(signatureAlgorithm)
        val context = mapOf(
                "matrix" to mapOf(
                        "token" to openIdToken.accessToken,
                        "room_id" to roomId,
                        "server_name" to openIdToken.matrixServerName
                ),
                "user" to mapOf(
                        "name" to userDisplayName,
                        "avatar" to userAvatarUrl
                )
        )
        // As per Jitsi token auth, `iss` needs to be set to something agreed between
        // JWT generating side and Prosody config. Since we have no configuration for
        // the widgets, we can't set one anywhere. Using the Jitsi domain here probably makes sense.
        val token = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer(jitsiServerDomain)
                .setSubject(jitsiServerDomain)
                .setAudience(jitsiServerDomain.ensureProtocol())
                // room is not used at the moment, a * works here.
                .claim("room", "*")
                .claim("context", context)
                .signWith(key)
                .compact()
        // Re-sign token with the hardcoded key
        val toSign = token.substring(0, token.lastIndexOf('.'))
        val mac = Mac.getInstance(signatureAlgorithm.jcaName)
        mac.init(SecretKeySpec("notused".toByteArray(), mac.algorithm))
        val prosodySignature = Encoders.BASE64URL.encode(mac.doFinal(toSign.toByteArray()))
        return "$toSign.$prosodySignature"
    }
}
