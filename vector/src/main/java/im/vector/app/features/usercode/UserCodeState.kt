/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.util.MatrixItem

data class UserCodeState(
        val userId: String,
        val matrixItem: MatrixItem? = null,
        val shareLink: String? = null,
        val mode: Mode = Mode.SHOW
) : MavericksState {
    sealed class Mode {
        object SHOW : Mode()
        object SCAN : Mode()
        data class RESULT(val matrixItem: MatrixItem, val rawLink: String) : Mode()
    }

    constructor(args: UserCodeActivity.Args) : this(
            userId = args.userId
    )
}
