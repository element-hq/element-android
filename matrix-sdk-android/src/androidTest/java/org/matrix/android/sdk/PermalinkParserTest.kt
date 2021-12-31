/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk

import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser

@FixMethodOrder(MethodSorters.JVM)
class PermalinkParserTest {

    @Test
    fun testParseEmailInvite() {
        val rawInvite = """
            https://app.element.io/#/room/%21MRBNLPtFnMAazZVPMO%3Amatrix.org?email=bob%2Bspace%40example.com&signurl=https%3A%2F%2Fvector.im%2F_matrix%2Fidentity%2Fapi%2Fv1%2Fsign-ed25519%3Ftoken%3DXmOwRZnSFabCRhTywFbJWKXWVNPysOpXIbroMGaUymqkJSvHeVKRsjHajwjCYdBsvGSvHauxbKfJmOxtXldtyLnyBMLKpBQCMzyYggrdapbVIceWZBtmslOQrXLABRoe%26private_key%3DT2gq0c3kJB_8OroXVxl1pBnzHsN7V6Xn4bEBSeW1ep4&room_name=Team2&room_avatar_url=&inviter_name=hiphop5&guest_access_token=&guest_user_id=
        """.trimIndent()
                .replace("https://app.element.io/#/room/", "https://matrix.to/#/")

        val parsedLink = PermalinkParser.parse(rawInvite)
        Assert.assertTrue("Should be parsed as email invite but was ${parsedLink::class.java}", parsedLink is PermalinkData.RoomEmailInviteLink)
        parsedLink as PermalinkData.RoomEmailInviteLink
        Assert.assertEquals("!MRBNLPtFnMAazZVPMO:matrix.org", parsedLink.roomId)
        Assert.assertEquals("XmOwRZnSFabCRhTywFbJWKXWVNPysOpXIbroMGaUymqkJSvHeVKRsjHajwjCYdBsvGSvHauxbKfJmOxtXldtyLnyBMLKpBQCMzyYggrdapbVIceWZBtmslOQrXLABRoe", parsedLink.token)
        Assert.assertEquals("vector.im", parsedLink.identityServer)
        Assert.assertEquals("Team2", parsedLink.roomName)
        Assert.assertEquals("hiphop5", parsedLink.inviterName)
    }

    @Test
    fun testParseLinkWIthEvent() {
        val rawInvite = "https://matrix.to/#/!OGEhHVWSdvArJzumhm:matrix.org/\$xuvJUVDJnwEeVjPx029rAOZ50difpmU_5gZk_T0jGfc?via=matrix.org&via=libera.chat&via=matrix.example.io"

        val parsedLink = PermalinkParser.parse(rawInvite)
        Assert.assertTrue("Should be parsed as room link", parsedLink is PermalinkData.RoomLink)
        parsedLink as PermalinkData.RoomLink
        Assert.assertEquals("!OGEhHVWSdvArJzumhm:matrix.org", parsedLink.roomIdOrAlias)
        Assert.assertEquals("\$xuvJUVDJnwEeVjPx029rAOZ50difpmU_5gZk_T0jGfc", parsedLink.eventId)
        Assert.assertEquals(3, parsedLink.viaParameters.size)
        Assert.assertTrue(parsedLink.viaParameters.contains("matrix.example.io"))
        Assert.assertTrue(parsedLink.viaParameters.contains("matrix.org"))
        Assert.assertTrue(parsedLink.viaParameters.contains("matrix.example.io"))
    }
}
