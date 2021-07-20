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

import im.vector.app.core.platform.VectorViewEvents

sealed class UserCodeShareViewEvents : VectorViewEvents {
    object Dismiss : UserCodeShareViewEvents()
    object ShowWaitingScreen : UserCodeShareViewEvents()
    object HideWaitingScreen : UserCodeShareViewEvents()
    data class ToastMessage(val message: String) : UserCodeShareViewEvents()
    data class NavigateToRoom(val roomId: String) : UserCodeShareViewEvents()
    data class CameraPermissionNotGranted(val deniedPermanently: Boolean) : UserCodeShareViewEvents()
    data class SharePlainText(val text: String, val title: String, val richPlainText: String) : UserCodeShareViewEvents()
}
