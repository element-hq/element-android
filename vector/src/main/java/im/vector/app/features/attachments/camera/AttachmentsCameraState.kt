/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.attachments.camera

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.airbnb.mvrx.MavericksState

data class AttachmentsCameraState(
        val captureMode: MediaType = MediaType.IMAGE,
        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
        val rotation: Int = Surface.ROTATION_0,
) : MavericksState
