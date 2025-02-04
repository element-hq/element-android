/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.attachments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.core.dialogs.PhotoOrVideoDialog
import im.vector.app.core.platform.Restorable
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.compat.getParcelableCompat
import im.vector.lib.core.utils.compat.getSerializableCompat
import im.vector.lib.multipicker.MultiPicker
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import timber.log.Timber

private const val CAPTURE_PATH_KEY = "CAPTURE_PATH_KEY"
private const val PENDING_TYPE_KEY = "PENDING_TYPE_KEY"

/**
 * This class helps to handle attachments by providing simple methods.
 */
class AttachmentsHelper(
        val context: Context,
        val callback: Callback,
        private val buildMeta: BuildMeta,
) : Restorable {

    interface Callback {
        fun onContactAttachmentReady(contactAttachment: ContactAttachment)
        fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>)
        fun onAttachmentError(throwable: Throwable)
    }

    // Capture path allows to handle camera image picking. It must be restored if the activity gets killed.
    private var captureUri: Uri? = null

    // The pending type is set if we have to handle permission request. It must be restored if the activity gets killed.
    var pendingType: AttachmentType? = null

    // Restorable

    override fun onSaveInstanceState(outState: Bundle) {
        captureUri?.also {
            outState.putParcelable(CAPTURE_PATH_KEY, it)
        }
        pendingType?.also {
            outState.putSerializable(PENDING_TYPE_KEY, it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        captureUri = savedInstanceState?.getParcelableCompat(CAPTURE_PATH_KEY)
        pendingType = savedInstanceState?.getSerializableCompat(PENDING_TYPE_KEY)
    }

    // Public Methods

    /**
     * Starts the process for handling file picking.
     */
    fun selectFile(activityResultLauncher: ActivityResultLauncher<Intent>) = doSafe {
        MultiPicker.get(MultiPicker.FILE).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling image/video picking.
     */
    fun selectGallery(activityResultLauncher: ActivityResultLauncher<Intent>) = doSafe {
        MultiPicker.get(MultiPicker.MEDIA).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling audio picking.
     */
    fun selectAudio(activityResultLauncher: ActivityResultLauncher<Intent>) = doSafe {
        MultiPicker.get(MultiPicker.AUDIO).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling image/video capture. Can open a dialog
     */
    fun openCamera(
            activity: Activity,
            vectorPreferences: VectorPreferences,
            cameraActivityResultLauncher: ActivityResultLauncher<Intent>,
            cameraVideoActivityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        PhotoOrVideoDialog(activity, vectorPreferences).show(object : PhotoOrVideoDialog.PhotoOrVideoDialogListener {
            override fun takePhoto() = doSafe {
                captureUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(context, cameraActivityResultLauncher)
            }

            override fun takeVideo() = doSafe {
                captureUri = MultiPicker.get(MultiPicker.CAMERA_VIDEO).startWithExpectingFile(context, cameraVideoActivityResultLauncher)
            }
        })
    }

    /**
     * Starts the process for handling contact picking.
     */
    fun selectContact(activityResultLauncher: ActivityResultLauncher<Intent>) = doSafe {
        MultiPicker.get(MultiPicker.CONTACT).startWith(activityResultLauncher)
    }

    private fun doSafe(function: () -> Unit) {
        try {
            function()
        } catch (activityNotFound: ActivityNotFoundException) {
            callback.onAttachmentError(activityNotFound)
        }
    }

    /**
     * This methods aims to handle the result data.
     */
    fun onFileResult(data: Intent?) {
        callback.onContentAttachmentsReady(
                MultiPicker.get(MultiPicker.FILE)
                        .getSelectedFiles(context, data)
                        .map { it.toContentAttachmentData() }
        )
    }

    fun onAudioResult(data: Intent?) {
        callback.onContentAttachmentsReady(
                MultiPicker.get(MultiPicker.AUDIO)
                        .getSelectedFiles(context, data)
                        .map { it.toContentAttachmentData() }
        )
    }

    fun onContactResult(data: Intent?) {
        MultiPicker.get(MultiPicker.CONTACT)
                .getSelectedFiles(context, data)
                .firstOrNull()
                ?.toContactAttachment()
                ?.let {
                    if (buildMeta.lowPrivacyLoggingEnabled) {
                        Timber.v("On contact attachment ready: $it")
                    }
                    callback.onContactAttachmentReady(it)
                }
    }

    fun onMediaResult(data: Intent?) {
        callback.onContentAttachmentsReady(
                MultiPicker.get(MultiPicker.MEDIA)
                        .getSelectedFiles(context, data)
                        .map { it.toContentAttachmentData() }
        )
    }

    fun onCameraResult() {
        captureUri?.let { captureUri ->
            MultiPicker.get(MultiPicker.CAMERA)
                    .getTakenPhoto(context, captureUri)
                    ?.let {
                        callback.onContentAttachmentsReady(
                                listOf(it).map { it.toContentAttachmentData() }
                        )
                    }
        }
    }

    fun onCameraVideoResult() {
        captureUri?.let { captureUri ->
            MultiPicker.get(MultiPicker.CAMERA_VIDEO)
                    .getTakenVideo(context, captureUri)
                    ?.let {
                        callback.onContentAttachmentsReady(
                                listOf(it).map { it.toContentAttachmentData() }
                        )
                    }
        }
    }

    fun onVideoResult(data: Intent?) {
        callback.onContentAttachmentsReady(
                MultiPicker.get(MultiPicker.VIDEO)
                        .getSelectedFiles(context, data)
                        .map { it.toContentAttachmentData() }
        )
    }
}
