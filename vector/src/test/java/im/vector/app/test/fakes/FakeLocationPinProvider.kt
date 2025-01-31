/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.graphics.drawable.Drawable
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk

class FakeLocationPinProvider {

    val instance = mockk<LocationPinProvider>(relaxed = true)

    fun givenCreateForUserId(userId: String, expectedDrawable: Drawable) {
        every { instance.create(userId, captureLambda()) } answers { lambda<(Drawable) -> Unit>().invoke(expectedDrawable) }
    }
}
