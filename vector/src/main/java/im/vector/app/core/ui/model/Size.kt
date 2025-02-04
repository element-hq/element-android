/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.model

import androidx.annotation.Px

// android.util.Size in API 21+
data class Size(@Px val width: Int, @Px val height: Int)
