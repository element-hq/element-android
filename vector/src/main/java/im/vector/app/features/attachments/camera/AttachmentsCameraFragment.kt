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
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.time.Clock
import im.vector.app.databinding.FragmentAttachmentsCameraBinding
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class AttachmentsCameraFragment :
        VectorBaseFragment<FragmentAttachmentsCameraBinding>(),
        VectorMenuProvider {

    @Inject lateinit var clock: Clock
    private val viewModel: AttachmentsCameraModel by activityViewModel()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentLens: Int? = null

    private lateinit var camera: Camera
    private lateinit var cameraExecutor: ExecutorService

    private val gestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
            camera.cameraControl.setZoomRatio(scale)
            return true
        }
    }

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

    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                withState(viewModel) { state ->
                    val rotation = when (orientation) {
                        in 45 until 135 -> Surface.ROTATION_270
                        in 135 until 225 -> Surface.ROTATION_180
                        in 225 until 315 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }
                    if (rotation != state.rotation) {
                        viewModel.handle(AttachmentsCameraAction.SetRotation(rotation))
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents {
            when (it) {
                AttachmentsCameraViewEvents.StartRecording -> {
                    views.attachmentsCameraCaptureAction.setImageDrawable(
                            context?.getDrawable(R.drawable.ic_video_off)
                    )
                    views.attachmentsCameraChangeAction.isEnabled = false
                    views.attachmentsCameraFlip.isEnabled = false
                }
                AttachmentsCameraViewEvents.TakePhoto -> views.attachmentsCameraLoading.isVisible = true
                AttachmentsCameraViewEvents.SetErrorAndFinish -> (activity as AttachmentsCameraActivity).setErrorAndFinish()
                is AttachmentsCameraViewEvents.SetResultAndFinish -> (activity as AttachmentsCameraActivity).setResultAndFinish(it.vectorCameraOutput)
            }
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        orientationEventListener.enable()

        views.attachmentsCameraCaptureAction.debouncedClicks {
            context?.let {
                viewModel.handle(AttachmentsCameraAction.Capture(it, imageCapture, videoCapture))
            }
        }

        views.attachmentsCameraChangeAction.debouncedClicks {
            viewModel.handle(AttachmentsCameraAction.ChangeCaptureMode)
        }

        views.attachmentsCameraFlip.debouncedClicks {
            viewModel.handle(AttachmentsCameraAction.ChangeLensFacing)
        }

        views.attachmentsCameraFlash.debouncedClicks {
            viewModel.handle(AttachmentsCameraAction.RotateFlashMode)
        }

        val scaleGestureDetector = ScaleGestureDetector(context, gestureListener)

        views.root.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun invalidate() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        }
        setCaptureModeButtons()
        setFlashButton()
        setRotation()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { permission ->
        context?.let { context ->
            ContextCompat.checkSelfPermission(context, permission)
        } == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setCaptureModeButtons() {
        withState(viewModel) { state ->
            when (state.captureMode) {
                MediaType.VIDEO -> {
                    views.attachmentsCameraCaptureAction.setImageDrawable(
                            context?.getDrawable(R.drawable.ic_video)
                    )
                    views.attachmentsCameraChangeAction.apply {
                        setImageDrawable(
                                context?.getDrawable(R.drawable.ic_camera_plain)
                        )
                        contentDescription = getString(R.string.attachment_camera_photo)
                    }
                }
                MediaType.IMAGE -> {
                    views.attachmentsCameraCaptureAction.setImageDrawable(
                            context?.getDrawable(R.drawable.ic_camera_plain)
                    )
                    views.attachmentsCameraChangeAction.apply {
                        setImageDrawable(
                                context?.getDrawable(R.drawable.ic_video)
                        )
                        contentDescription = getString(R.string.attachment_camera_video)
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setRotation() {
        withState(viewModel) { state ->
            arrayOf(
                    views.attachmentsCameraFlip,
                    views.attachmentsCameraFlash,
                    views.attachmentsCameraChangeAction,
                    views.attachmentsCameraCaptureAction,
            ).forEach {
                it.rotation = when (state.rotation) {
                    Surface.ROTATION_270 -> 270F
                    Surface.ROTATION_180 -> 180F
                    Surface.ROTATION_90 -> 90F
                    else -> 0F
                }
            }
            imageCapture?.targetRotation = state.rotation
            videoCapture?.targetRotation = state.rotation
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setFlashButton() {
        withState(viewModel) { state ->
            if (state.captureMode == MediaType.VIDEO || state.cameraSelector != CameraSelector.DEFAULT_BACK_CAMERA) {
                views.attachmentsCameraFlash.isVisible = false
            } else {
                views.attachmentsCameraFlash.apply {
                    isVisible = true
                    setImageDrawable(
                            context?.getDrawable(
                                    when (state.flashMode) {
                                        ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
                                        ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_flash_off
                                        ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                                        else -> R.drawable.ic_flash_auto
                                    }
                            )
                    )
                    contentDescription = context?.getString(
                            when (state.flashMode) {
                                ImageCapture.FLASH_MODE_AUTO -> R.string.attachment_camera_disable_flash
                                ImageCapture.FLASH_MODE_OFF -> R.string.attachment_camera_enable_flash
                                ImageCapture.FLASH_MODE_ON -> R.string.attachment_camera_auto_flash
                                else -> R.string.attachment_camera_disable_flash
                            }
                    )
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        Timber.d("Starting Camera")
        val context = this.context ?: return
        withState(viewModel) { state ->
            if (currentLens == state.cameraSelector.lensFacing) return@withState
            currentLens = state.cameraSelector.lensFacing

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

                imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                videoCapture = VideoCapture.withOutput(recorder)

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    camera = cameraProvider.bindToLifecycle(
                            this, state.cameraSelector, preview, imageCapture, videoCapture
                    )

                    Timber.d("Lensfacing: ${camera.cameraInfo.cameraSelector}")
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
                mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.toTypedArray()
    }
}
