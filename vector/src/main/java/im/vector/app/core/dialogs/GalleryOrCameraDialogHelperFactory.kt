/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.dialogs

import androidx.fragment.app.Fragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.time.Clock
import javax.inject.Inject

/**
 * Factory for [GalleryOrCameraDialogHelper].
 */
class GalleryOrCameraDialogHelperFactory @Inject constructor(
        private val colorProvider: ColorProvider,
        private val clock: Clock,
) {
    fun create(fragment: Fragment): GalleryOrCameraDialogHelper {
        return GalleryOrCameraDialogHelper(fragment, colorProvider, clock)
    }
}
