/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.mentions

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import io.element.android.wysiwyg.display.TextDisplay
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.MatrixItem.Companion.NOTIFY_EVERYONE
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PillDisplayHandlerTest {
    private val mockGetMember = mockk<(userId: String) -> RoomMemberSummary?>()
    private val mockGetRoom = mockk<(roomId: String) -> RoomSummary?>()
    private val fakeReplacementSpanFactory = { matrixItem: MatrixItem -> MatrixItemHolderSpan(matrixItem) }

    private companion object {
        const val ROOM_ID = "!thisroom:matrix.org"
        const val NON_MATRIX_URL = "https://example.com"
        const val UNKNOWN_MATRIX_ROOM_ID = "!unknown:matrix.org"
        const val UNKNOWN_MATRIX_ROOM_URL = "https://matrix.to/#/$UNKNOWN_MATRIX_ROOM_ID"
        const val KNOWN_MATRIX_ROOM_ID = "!known:matrix.org"
        const val KNOWN_MATRIX_ROOM_URL = "https://matrix.to/#/$KNOWN_MATRIX_ROOM_ID"
        const val KNOWN_MATRIX_ROOM_AVATAR = "https://example.com/avatar.png"
        const val KNOWN_MATRIX_ROOM_NAME = "known room"
        const val UNKNOWN_MATRIX_USER_ID = "@unknown:matrix.org"
        const val UNKNOWN_MATRIX_USER_URL = "https://matrix.to/#/$UNKNOWN_MATRIX_USER_ID"
        const val KNOWN_MATRIX_USER_ID = "@known:matrix.org"
        const val KNOWN_MATRIX_USER_URL = "https://matrix.to/#/$KNOWN_MATRIX_USER_ID"
        const val KNOWN_MATRIX_USER_AVATAR = "https://example.com/avatar.png"
        const val KNOWN_MATRIX_USER_NAME = "known user"
        const val CUSTOM_DOMAIN_MATRIX_ROOM_URL = "https://customdomain/#/room/$KNOWN_MATRIX_ROOM_ID"
        const val CUSTOM_DOMAIN_MATRIX_USER_URL = "https://customdomain.com/#/user/$KNOWN_MATRIX_USER_ID"
        const val KNOWN_MATRIX_ROOM_ALIAS = "#known-alias:matrix.org"
        const val KNOWN_MATRIX_ROOM_ALIAS_URL = "https://matrix.to/#/$KNOWN_MATRIX_ROOM_ALIAS"
    }

    @Before
    fun setUp() {
        every { mockGetMember(UNKNOWN_MATRIX_USER_ID) } returns null
        every { mockGetMember(KNOWN_MATRIX_USER_ID) } returns createFakeRoomMember(KNOWN_MATRIX_USER_NAME, KNOWN_MATRIX_USER_ID, KNOWN_MATRIX_USER_AVATAR)
        every { mockGetRoom(UNKNOWN_MATRIX_ROOM_ID) } returns null
        every { mockGetRoom(KNOWN_MATRIX_ROOM_ID) } returns createFakeRoom(KNOWN_MATRIX_ROOM_ID, KNOWN_MATRIX_ROOM_NAME, KNOWN_MATRIX_ROOM_AVATAR)
        every { mockGetRoom(ROOM_ID) } returns createFakeRoom(ROOM_ID, KNOWN_MATRIX_ROOM_NAME, KNOWN_MATRIX_ROOM_AVATAR)
        every { mockGetRoom(KNOWN_MATRIX_ROOM_ALIAS) } returns createFakeRoomWithAlias(
                KNOWN_MATRIX_ROOM_ALIAS,
                KNOWN_MATRIX_ROOM_ID,
                KNOWN_MATRIX_ROOM_NAME,
                KNOWN_MATRIX_ROOM_AVATAR
        )
    }

    @Test
    fun `when resolve non-matrix link, then it returns plain text`() {
        val subject = createSubject()

        val result = subject.resolveMentionDisplay("text", NON_MATRIX_URL)

        assertEquals(TextDisplay.Plain, result)
    }

    @Test
    fun `when resolve unknown user link, then it returns generic custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", UNKNOWN_MATRIX_USER_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.UserItem(UNKNOWN_MATRIX_USER_ID, UNKNOWN_MATRIX_USER_ID, null), matrixItem)
    }

    @Test
    fun `when resolve known user link, then it returns named custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", KNOWN_MATRIX_USER_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.UserItem(KNOWN_MATRIX_USER_ID, KNOWN_MATRIX_USER_NAME, KNOWN_MATRIX_USER_AVATAR), matrixItem)
    }

    @Test
    fun `when resolve unknown room link, then it returns generic custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", UNKNOWN_MATRIX_ROOM_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.RoomItem(UNKNOWN_MATRIX_ROOM_ID, UNKNOWN_MATRIX_ROOM_ID, null), matrixItem)
    }

    @Test
    fun `when resolve known room link, then it returns named custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", KNOWN_MATRIX_ROOM_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.RoomItem(KNOWN_MATRIX_ROOM_ID, KNOWN_MATRIX_ROOM_NAME, KNOWN_MATRIX_ROOM_AVATAR), matrixItem)
    }

    @Test
    fun `when resolve @room link, then it returns room notification custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("@room", KNOWN_MATRIX_ROOM_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.EveryoneInRoomItem(KNOWN_MATRIX_ROOM_ID, NOTIFY_EVERYONE, KNOWN_MATRIX_ROOM_AVATAR, KNOWN_MATRIX_ROOM_NAME), matrixItem)
    }

    @Test
    fun `when resolve @room keyword, then it returns room notification custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveAtRoomMentionDisplay()
                .getMatrixItem()

        assertEquals(MatrixItem.EveryoneInRoomItem(ROOM_ID, NOTIFY_EVERYONE, KNOWN_MATRIX_ROOM_AVATAR, KNOWN_MATRIX_ROOM_NAME), matrixItem)
    }

    @Test
    fun `given cannot get current room, when resolve @room keyword, then it returns room notification custom pill`() {
        val subject = createSubject()
        every { mockGetRoom(ROOM_ID) } returns null

        val matrixItem = subject.resolveAtRoomMentionDisplay()
                .getMatrixItem()

        assertEquals(MatrixItem.EveryoneInRoomItem(ROOM_ID, NOTIFY_EVERYONE, null, null), matrixItem)
    }

    @Test
    fun `when resolve known user for custom domain link, then it returns named custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", CUSTOM_DOMAIN_MATRIX_USER_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.UserItem(KNOWN_MATRIX_USER_ID, KNOWN_MATRIX_USER_NAME, KNOWN_MATRIX_USER_AVATAR), matrixItem)
    }

    @Test
    fun `when resolve known room for custom domain link, then it returns named custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", CUSTOM_DOMAIN_MATRIX_ROOM_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.RoomItem(KNOWN_MATRIX_ROOM_ID, KNOWN_MATRIX_ROOM_NAME, KNOWN_MATRIX_ROOM_AVATAR), matrixItem)
    }

    @Test
    fun `when resolve known room with alias link, then it returns named custom pill`() {
        val subject = createSubject()

        val matrixItem = subject.resolveMentionDisplay("text", KNOWN_MATRIX_ROOM_ALIAS_URL)
                .getMatrixItem()

        assertEquals(MatrixItem.RoomAliasItem(KNOWN_MATRIX_ROOM_ALIAS, KNOWN_MATRIX_ROOM_NAME, KNOWN_MATRIX_ROOM_AVATAR), matrixItem)
    }

    private fun TextDisplay.getMatrixItem(): MatrixItem {
        val customSpan = this as? TextDisplay.Custom
        assertNotNull("The URL did not resolve to a custom link display method", customSpan)

        val matrixItemHolderSpan = customSpan!!.customSpan as MatrixItemHolderSpan
        return matrixItemHolderSpan.matrixItem
    }

    private fun createSubject(): PillDisplayHandler = PillDisplayHandler(
            roomId = ROOM_ID,
            getRoom = mockGetRoom,
            getMember = mockGetMember,
            replacementSpanFactory = fakeReplacementSpanFactory
    )

    private fun createFakeRoomMember(displayName: String, userId: String, avatarUrl: String): RoomMemberSummary = RoomMemberSummary(
            membership = Membership.JOIN,
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
    )

    private fun createFakeRoom(roomId: String, roomName: String, avatarUrl: String): RoomSummary = RoomSummary(
            roomId = roomId,
            displayName = roomName,
            avatarUrl = avatarUrl,
            encryptionEventTs = null,
            typingUsers = emptyList(),
            isEncrypted = false
    )

    private fun createFakeRoomWithAlias(roomAlias: String, roomId: String, roomName: String, avatarUrl: String): RoomSummary = RoomSummary(
            roomId = roomId,
            displayName = roomName,
            avatarUrl = avatarUrl,
            encryptionEventTs = null,
            typingUsers = emptyList(),
            isEncrypted = false,
            canonicalAlias = roomAlias
    )

    data class MatrixItemHolderSpan(
            val matrixItem: MatrixItem
    ) : ReplacementSpan() {
        override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            // Do nothing
        }

        override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            return 0
        }
    }
}
