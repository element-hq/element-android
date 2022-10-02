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
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

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
        when (action) {
            AttachmentsCameraAction.ChangeLensFacing -> changeLensFacing()
            AttachmentsCameraAction.RotateFlashMode -> rotateFlashMode()
            is AttachmentsCameraAction.SetRotation -> setRotation(action.rotation)
            is AttachmentsCameraAction.Capture -> capture(action)
        }
    }

    private fun changeLensFacing() {
        setState {
            copy(
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
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
            copy(
                    rotation = newRotation
            )
        }
    }

    private fun capture(action: AttachmentsCameraAction.Capture) {
        recording?.let {
            recording?.stop()
            recording = null
            return
        }
        action.videoCapture?.let {
            capture(action.context, action.videoCapture)
            return
        }
        action.imageCapture?.let {
            capture(action.context, action.imageCapture)
            return
        }
        _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
    }

    private fun capture(context: Context, imageCapture: ImageCapture) {
        withState { state ->
            imageCapture.flashMode = state.flashMode

            val file = createTempFile(context, MediaType.IMAGE)
            val outputUri = getUri(context, file)

            imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        @SuppressLint("RestrictedApi")
                        override fun onCaptureSuccess(image: ImageProxy) {
                            setState { copy( done = true ) }
                            val orientation =  (
                                    (imageCapture.camera?.cameraInfo?.sensorRotationDegrees?.toFloat() ?: 0F) - when (state.rotation) {
                                        Surface.ROTATION_270 -> 270F
                                        Surface.ROTATION_180 -> 180F
                                        Surface.ROTATION_90 -> 90F
                                        else -> 0F
                                    })
                            saveImageProxyToFile(image, file, orientation)?.let {
                                _viewEvents.post(
                                        AttachmentsCameraViewEvents.SetResultAndFinish(
                                                AttachmentsCameraOutput(
                                                        type = MediaType.IMAGE,
                                                        uri = outputUri
                                                )
                                        )
                                )
                            } ?: _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Timber.e("Photo capture failed: ${exception.message}", exception)
                            Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show()
                            _viewEvents.post(AttachmentsCameraViewEvents.SetErrorAndFinish)
                        }
                    }
            )
        }
    }

    private fun saveImageProxyToFile(image: ImageProxy, file: File, orientation: Float): Boolean? {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)?.let { bitmap ->
            val bos = ByteArrayOutputStream()
            val matrix = Matrix().apply {
                postRotate(orientation)
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0,0 , bitmap.width, bitmap.height, matrix, true)
            rotatedBitmap.compress(CompressFormat.JPEG, 90, bos)
            val fd = FileOutputStream(file)
            fd.write(bos.toByteArray())
            fd.flush()
            fd.close()
            true
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "RestrictedApi")
    private fun capture(context: Context, videoCapture: VideoCapture<Recorder>) = withState { state ->
        Timber.d("Capturing Video")

        videoCapture.camera?.cameraControl?.enableTorch(state.flashMode == ImageCapture.FLASH_MODE_ON)
        videoCapture.targetRotation = state.rotation

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
                            setState { copy( recording = true ) }
                        }
                        is VideoRecordEvent.Finalize -> {
                            setState { copy( done = true ) }
                            if (!recordEvent.hasError()) {
                                Timber.d(
                                        "Video capture succeeded: " +
                                                "${recordEvent.outputResults.outputUri}"
                                )
                                _viewEvents.post(
                                        AttachmentsCameraViewEvents.SetResultAndFinish(
                                                AttachmentsCameraOutput(
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
