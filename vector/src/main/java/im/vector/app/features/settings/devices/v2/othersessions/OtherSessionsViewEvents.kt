/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import im.vector.app.core.platform.VectorViewEvents

sealed class OtherSessionsViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : OtherSessionsViewEvents()
    data class Failure(val throwable: Throwable) : OtherSessionsViewEvents()
}
