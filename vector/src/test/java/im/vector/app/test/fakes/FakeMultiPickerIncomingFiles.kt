/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.content.Intent
import im.vector.app.features.attachments.MultiPickerIncomingFiles
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

class FakeMultiPickerIncomingFiles {

    val instance = mockk<MultiPickerIncomingFiles>()

    fun givenFileReturns(intent: Intent, result: List<ContentAttachmentData>) {
        every { instance.file(intent) } returns result
    }

    fun givenAudioReturns(intent: Intent, result: List<ContentAttachmentData>) {
        every { instance.audio(intent) } returns result
    }

    fun givenVideoReturns(intent: Intent, result: List<ContentAttachmentData>) {
        every { instance.video(intent) } returns result
    }

    fun givenImageReturns(intent: Intent, result: List<ContentAttachmentData>) {
        every { instance.image(intent) } returns result
    }
}
