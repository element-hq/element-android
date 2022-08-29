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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.time.Clock
import timber.log.Timber
import java.io.File

class AttachmentsCameraModel @AssistedInject constructor(
        @Assisted val initialState: AttachmentsCameraState,
        val clock: Clock
) : VectorViewModel<AttachmentsCameraState, AttachmentsCameraAction, AttachmentsCameraViewEvents>(initialState) {

    private var recording: Recording? = null

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AttachmentsCameraModel, AttachmentsCameraState> {
        override fun create(initialState: AttachmentsCameraState): AttachmentsCameraModel
    }

    companion object : MavericksViewModelFactory<AttachmentsCameraModel, AttachmentsCameraState> by hiltMavericksViewModelFactory()

    override fun handle(action: AttachmentsCameraAction) {
        when(action) {
            AttachmentsCameraAction.ChangeLensFacing -> changeLensFacing()
            AttachmentsCameraAction.ChangeCaptureMode -> changeCaptureMode()
            AttachmentsCameraAction.RotateFlashMode -> rotateFlashMode()
            is AttachmentsCameraAction.SetRotation -> setRotation(action.rotation)
            is AttachmentsCameraAction.Capture -> capture(action)
        }
    }

    private fun changeLensFacing() {
        setState {
            copy (
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            )
        }
    }

    private fun changeCaptureMode() {
        setState {
            copy(
                    captureMode = when (captureMode) {
                        MediaType.IMAGE -> MediaType.VIDEO
                        MediaType.VIDEO -> MediaType.IMAGE
                    }
            )
        }
    }

    private fun rotateFlashMode() {
        setState {
            copy(
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
            )
        }
    }

    private fun setRotation(newRotation: Int) {
        setState {
            copy (
                    rotation = newRotation
            )
        }
    }

    private fun capture(action: AttachmentsCameraAction.Capture) = withState { state ->
        when(state.captureMode) {
            MediaType.IMAGE -> {
                action.imageCapture?.let {
                    capture(action.context, action.imageCapture)
                    true
                }
            }
            MediaType.VIDEO -> {
                action.videoCapture?.let {
                    capture(action.context, action.videoCapture)
                    true
                }
            }
        } ?: _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
    }

    private fun capture(context: Context, imageCapture: ImageCapture) {
        _viewEvents.post(AttachmentsCameraViewEvents.TakePhoto)
        withState { state ->
            imageCapture.flashMode = state.flashMode

            val file = createTempFile(context, MediaType.IMAGE)
            val outputUri = getUri(context, file)

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(file)
                    .build()

            // Set up image capture listener, which is triggered after photo has
            // been taken
            imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Timber.e("Photo capture failed: ${exc.message}", exc)
                            Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show()
                            _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            _viewEvents.post(
                                    AttachmentsCameraViewEvents.SetResultAndFinish(
                                            VectorCameraOutput(
                                                type = MediaType.IMAGE,
                                                uri = outputUri
                                            )
                                    )
                            )
                        }
                    }
            )
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun capture(context: Context, videoCapture: VideoCapture<Recorder>) {
        Timber.d("Capturing Video")
        recording?.let {
            recording?.stop()
            recording = null
            return
        }

        val file = createTempFile(context, MediaType.VIDEO)
        val outputUri = getUri(context, file)

        val options = FileOutputOptions
                .Builder(file)
                .build()

        recording = videoCapture.output
                .prepareRecording(context, options)
                .apply {
                    if (PermissionChecker.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                            ) == PermissionChecker.PERMISSION_GRANTED) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _viewEvents.post(AttachmentsCameraViewEvents.StartRecording)
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Timber.d(
                                        "Video capture succeeded: " +
                                                "${recordEvent.outputResults.outputUri}"
                                )
                                _viewEvents.post(
                                        AttachmentsCameraViewEvents.SetResultAndFinish(
                                                VectorCameraOutput(
                                                        type = MediaType.VIDEO,
                                                        uri = outputUri
                                                )
                                        )
                                )
                            } else {
                                recording?.close()
                                recording = null
                                Timber.e(
                                        "Video capture ends with error: " +
                                                "${recordEvent.error}"
                                )
                                _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
                            }
                        }
                    }
                }
    }

    private fun createTempFile(context: Context, type: MediaType): File {
        var prefix = ""
        var suffix = ""
        when (type) {
            MediaType.IMAGE -> {
                prefix = "IMG_"
                suffix = ".jpg"
            }
            MediaType.VIDEO -> {
                prefix = "VID_"
                suffix = ".mp4"
            }
        }
        return File.createTempFile(
                "$prefix${clock.epochMillis()}",
                suffix,
                context.cacheDir.also { it?.mkdirs() }!!
        )
    }


    private fun getUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
                context,
                context.packageName + ".fileProvider",
                file
        )
    }
}
