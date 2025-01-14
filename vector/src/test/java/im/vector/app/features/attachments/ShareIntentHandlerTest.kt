/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import android.content.Intent
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeFunction1
import im.vector.app.test.fakes.FakeIntent
import im.vector.app.test.fakes.FakeMultiPickerIncomingFiles
import im.vector.app.test.fixtures.ContentAttachmentDataFixture.aContentAttachmentData
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

private val A_CONTEXT = FakeContext().instance
private const val A_PLAIN_TEXT_EXTRA = "plain text for sharing"
private val A_CONTENT_ATTACHMENT_LIST = listOf(aContentAttachmentData())

class ShareIntentHandlerTest {

    private val fakeMultiPickerIncomingFiles = FakeMultiPickerIncomingFiles()
    private val onFile = FakeFunction1<List<ContentAttachmentData>>()
    private val onPlainText = FakeFunction1<String>()

    private val shareIntentHandler = ShareIntentHandler(fakeMultiPickerIncomingFiles.instance, A_CONTEXT)

    @Test
    fun `given an unhandled sharing intent type, when handling intent, then is not handled`() {
        val unknownShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "unknown/type") }

        val handled = handleIncomingShareIntent(unknownShareIntent)

        onFile.verifyNoInteractions()
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo false
    }

    @Test
    fun `given a plain text sharing intent, when handling intent, then is handled and parses plain text content`() {
        val plainTextShareIntent = FakeIntent().also {
            it.givenResolvesType(A_CONTEXT, "text/plain")
            it.givenCharSequenceExtra(key = Intent.EXTRA_TEXT, value = A_PLAIN_TEXT_EXTRA)
        }

        val handled = handleIncomingShareIntent(plainTextShareIntent)

        onFile.verifyNoInteractions()
        onPlainText.assertValue(A_PLAIN_TEXT_EXTRA)
        handled shouldBeEqualTo true
    }

    @Test
    fun `given an empty plain text sharing intent, when handling intent, then is not handled`() {
        val plainTextShareIntent = FakeIntent().also {
            it.givenResolvesType(A_CONTEXT, "text/plain")
            it.givenCharSequenceExtra(key = Intent.EXTRA_TEXT, value = "")
        }

        val handled = handleIncomingShareIntent(plainTextShareIntent)

        onFile.verifyNoInteractions()
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo false
    }

    @Test
    fun `given an image sharing intent, when handling intent, then is handled and parses image files`() {
        val imageShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "image/png") }
        fakeMultiPickerIncomingFiles.givenImageReturns(imageShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(imageShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    @Test
    fun `given an audio sharing intent, when handling intent, then is handled and parses audio files`() {
        val audioShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "audio/mp3") }
        fakeMultiPickerIncomingFiles.givenAudioReturns(audioShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(audioShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    @Test
    fun `given an video sharing intent, when handling intent, then is handled and parses video files`() {
        val videoShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "video/mp4") }
        fakeMultiPickerIncomingFiles.givenVideoReturns(videoShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(videoShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    @Test
    fun `given a file sharing intent, when handling intent, then is handled and parses files`() {
        val fileShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "file/*") }
        fakeMultiPickerIncomingFiles.givenFileReturns(fileShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(fileShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    @Test
    fun `given a application sharing intent, when handling intent, then is handled and parses files`() {
        val fileShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "application/apk") }
        fakeMultiPickerIncomingFiles.givenFileReturns(fileShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        handleIncomingShareIntent(fileShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
    }

    @Test
    fun `given a text sharing intent, when handling intent, then is handled and parses text files`() {
        val fileShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "text/ics") }
        fakeMultiPickerIncomingFiles.givenFileReturns(fileShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(fileShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    @Test
    fun `given a wildcard sharing intent, when handling intent, then is handled and parses files`() {
        val fileShareIntent = FakeIntent().also { it.givenResolvesType(A_CONTEXT, "*/*") }
        fakeMultiPickerIncomingFiles.givenFileReturns(fileShareIntent.instance, A_CONTENT_ATTACHMENT_LIST)

        val handled = handleIncomingShareIntent(fileShareIntent)

        onFile.assertValue(A_CONTENT_ATTACHMENT_LIST)
        onPlainText.verifyNoInteractions()
        handled shouldBeEqualTo true
    }

    private fun handleIncomingShareIntent(intent: FakeIntent): Boolean {
        return shareIntentHandler.handleIncomingShareIntent(intent.instance, onFile.capture, onPlainText.capture)
    }
}
