/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
