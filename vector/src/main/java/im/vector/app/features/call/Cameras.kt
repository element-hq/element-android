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

package im.vector.app.features.call

enum class CameraType {
    FRONT,
    BACK
}

data class CameraProxy(
        val name: String,
        val type: CameraType
)

sealed class CaptureFormat(val width: Int, val height: Int, val fps: Int) {
    object HD : CaptureFormat(1280, 720, 30)
    object SD : CaptureFormat(640, 480, 30)
}
