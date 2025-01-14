/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.test.fakes.FakeUri
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

object ContentAttachmentDataFixture {

    fun aContentAttachmentData() = ContentAttachmentData(
            type = ContentAttachmentData.Type.AUDIO,
            queryUri = FakeUri().instance,
            mimeType = null,
    )
}
