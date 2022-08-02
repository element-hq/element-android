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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.time.Clock
import im.vector.app.databinding.FragmentAttachmentsCameraBinding
import im.vector.lib.multipicker.CameraUris
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class AttachmentsCameraFragment :
        VectorBaseFragment<FragmentAttachmentsCameraBinding>(),
        VectorMenuProvider {

    @Inject lateinit var clock: Clock

    private val cameraUris: CameraUris by args()

    private lateinit var authority : String
    private lateinit var storageDir : File

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null


    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            activity?.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authority = context?.packageName + ".fileProvider"
        storageDir = context?.cacheDir.also { it?.mkdirs() }!!

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        views.attachmentsCameraImageAction.debouncedClicks {
            takePhoto()
        }

        views.attachmentsCameraVideoAction.debouncedClicks {
            captureVideo()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { permission ->
        context?.let { context ->
            ContextCompat.checkSelfPermission(context, permission)
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
        Timber.d("Taking a photo")
        context?.let { context ->
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        val uri = cameraUris.photoUri ?: return
        val file = context.contentResolver.openOutputStream(uri) ?: return

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
                        (activity as? AttachmentsCameraActivity)?.setErrorAndFinish()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults){
                        (activity as? AttachmentsCameraActivity)?.setResultAndFinish(cameraUris.apply { videoUri = null })
                    }
                }
        )
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun captureVideo() {
        Timber.d("Capturing Video")
        context?.let { context ->
            val videoCapture = this.videoCapture ?: return

            views.attachmentsCameraImageAction.isEnabled = false

            val curRecording = recording
            if (curRecording != null) {
                // Stop the current recording session.
                curRecording.stop()
                recording = null
                return
            }

            val file = File.createTempFile(
                    "VID_${clock.epochMillis()}",
                    ".mp4",
                    storageDir
            )

            val outputUri = FileProvider.getUriForFile(
                    context,
                    authority,
                    file
            )

            val options = FileOutputOptions
                    .Builder(file)
                    .build()
            recording = videoCapture.output
                    .prepareRecording(context, options)
                    .apply {
                        if (PermissionChecker.checkSelfPermission(context,
                                        Manifest.permission.RECORD_AUDIO) ==
                                PermissionChecker.PERMISSION_GRANTED)
                        {
                            withAudioEnabled()
                        }
                    }
                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                        when(recordEvent) {
                            is VideoRecordEvent.Start -> {
                                views.attachmentsCameraVideoAction.setImageDrawable(
                                        context.getDrawable(R.drawable.ic_video_off)
                                )
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (!recordEvent.hasError()) {
                                    Timber.d("Video capture succeeded: " +
                                            "${recordEvent.outputResults.outputUri}")
                                    (activity as? AttachmentsCameraActivity)?.setResultAndFinish(
                                            cameraUris.apply {
                                                videoUri = outputUri
                                                photoUri = null
                                            }
                                    )
                                } else {
                                    recording?.close()
                                    recording = null
                                    Timber.e("Video capture ends with error: " +
                                            "${recordEvent.error}")
                                    (activity as? AttachmentsCameraActivity)?.setErrorAndFinish()
                                }
                            }
                        }
                    }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        Timber.d("Starting Camera")
        context?.let { context ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(views.viewFinder.surfaceProvider)
                        }

                imageCapture = ImageCapture.Builder().build()

                val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, videoCapture
                    )
                } catch (exc: Exception) {
                    Timber.e("Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    override fun onDestroyView() {
        cameraExecutor.shutdown()
        super.onDestroyView()
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAttachmentsCameraBinding {
        return FragmentAttachmentsCameraBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.vector_attachments_camera

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return true
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
                mutableListOf (
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.toTypedArray()

    }
}
