/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.util.MatrixItem

sealed class UserCodeActions : VectorViewModelAction {
    object DismissAction : UserCodeActions()
    data class SwitchMode(val mode: UserCodeState.Mode) : UserCodeActions()
    data class DecodedQRCode(val code: String) : UserCodeActions()
    data class StartChattingWithUser(val matrixItem: MatrixItem) : UserCodeActions()
    data class CameraPermissionNotGranted(val deniedPermanently: Boolean) : UserCodeActions()
    object ShareByText : UserCodeActions()
}
