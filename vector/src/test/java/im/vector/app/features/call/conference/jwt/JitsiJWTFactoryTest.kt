/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference.jwt

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.openid.OpenIdToken
import java.lang.reflect.ParameterizedType
import java.util.Base64

class JitsiJWTFactoryTest {
    private val base64Decoder = Base64.getUrlDecoder()
    private val moshi = Moshi.Builder().build()
    private val stringToString = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val stringToAny = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private lateinit var factory: JitsiJWTFactory

    @Before
    fun init() {
        factory = JitsiJWTFactory()
    }

    @Test
    fun `token contains 3 encoded parts`() {
        val token = createToken()

        val parts = token.split(".")
        assertEquals(3, parts.size)
        parts.forEach {
            assertTrue("Non-empty array", base64Decoder.decode(it).isNotEmpty())
        }
    }

    @Test
    fun `token contains unique signature`() {
        val signatures = listOf("one", "two").stream()
                .map { createToken(it) }
                .map { it.split(".")[2] }
                .map { base64Decoder.decode(it) }
                .toList()

        assertEquals(2, signatures.size)
        signatures.forEach {
            assertEquals(32, it.size)
        }
        assertFalse("Unique", signatures[0].contentEquals(signatures[1]))
    }

    @Test
    fun `token header contains algorithm`() {
        val token = createToken()

        assertEquals("HS256", parseTokenHeader(token)["alg"])
    }

    @Test
    fun `token header contains type`() {
        val token = createToken()

        assertEquals("JWT", parseTokenHeader(token)["typ"])
    }

    @Test
    fun `token body contains subject`() {
        val token = createToken()

        assertEquals("jitsi-server-domain", parseTokenBody(token)["sub"])
    }

    @Test
    fun `token body contains issuer`() {
        val token = createToken()

        assertEquals("jitsi-server-domain", parseTokenBody(token)["iss"])
    }

    @Test
    fun `token body contains audience`() {
        val token = createToken()

        assertEquals("https://jitsi-server-domain", parseTokenBody(token)["aud"])
    }

    @Test
    fun `token body contains room claim`() {
        val token = createToken()

        assertEquals("*", parseTokenBody(token)["room"])
    }

    @Test
    fun `token body contains matrix data`() {
        val token = createToken()

        assertEquals(mutableMapOf("room_id" to "room-id", "server_name" to "matrix-server-name", "token" to "matrix-token"), parseMatrixData(token))
    }

    @Test
    fun `token body contains user data`() {
        val token = createToken()

        assertEquals(mutableMapOf("name" to "user-display-name", "avatar" to "user-avatar-url"), parseUserData(token))
    }

    private fun createToken(): String {
        return createToken("matrix-token")
    }

    private fun createToken(accessToken: String): String {
        val openIdToken = OpenIdToken(accessToken, "matrix-token-type", "matrix-server-name", -1)
        return factory.create(openIdToken, "jitsi-server-domain", "room-id", "user-avatar-url", "user-display-name")
    }

    private fun parseTokenHeader(token: String): Map<String, String> {
        return parseTokenPart(token.split(".")[0], stringToString)
    }

    private fun parseTokenBody(token: String): Map<String, Any> {
        return parseTokenPart(token.split(".")[1], stringToAny)
    }

    private fun parseMatrixData(token: String): Map<*, *> {
        return (parseTokenBody(token)["context"] as Map<*, *>)["matrix"] as Map<*, *>
    }

    private fun parseUserData(token: String): Map<*, *> {
        return (parseTokenBody(token)["context"] as Map<*, *>)["user"] as Map<*, *>
    }

    private fun <T> parseTokenPart(value: String, type: ParameterizedType): T {
        val decoded = String(base64Decoder.decode(value))
        val adapter: JsonAdapter<T> = moshi.adapter(type)
        return adapter.fromJson(decoded)!!
    }
}
