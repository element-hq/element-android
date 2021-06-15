/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.attachments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.core.dialogs.PhotoOrVideoDialog
import im.vector.app.core.platform.Restorable
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.multipicker.MultiPicker
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import timber.log.Timber

private const val CAPTURE_PATH_KEY = "CAPTURE_PATH_KEY"
private const val PENDING_TYPE_KEY = "PENDING_TYPE_KEY"

/**
 * This class helps to handle attachments by providing simple methods.
 */
class AttachmentsHelper(val context: Context, val callback: Callback) : Restorable {

    interface Callback {
        fun onContactAttachmentReady(contactAttachment: ContactAttachment) {
            if (BuildConfig.LOG_PRIVATE_DATA) {
                Timber.v("On contact attachment ready: $contactAttachment")
            }
        }

        fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>)
        fun onAttachmentsProcessFailed()
    }

    // Capture path allows to handle camera image picking. It must be restored if the activity gets killed.
    private var captureUri: Uri? = null

    // The pending type is set if we have to handle permission request. It must be restored if the activity gets killed.
    var pendingType: AttachmentTypeSelectorView.Type? = null

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
        captureUri = savedInstanceState?.getParcelable(CAPTURE_PATH_KEY) as? Uri
        pendingType = savedInstanceState?.getSerializable(PENDING_TYPE_KEY) as? AttachmentTypeSelectorView.Type
    }

    // Public Methods

    /**
     * Starts the process for handling file picking
     */
    fun selectFile(activityResultLauncher: ActivityResultLauncher<Intent>) {
        MultiPicker.get(MultiPicker.FILE).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling image/video picking
     */
    fun selectGallery(activityResultLauncher: ActivityResultLauncher<Intent>) {
        MultiPicker.get(MultiPicker.MEDIA).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling audio picking
     */
    fun selectAudio(activityResultLauncher: ActivityResultLauncher<Intent>) {
        MultiPicker.get(MultiPicker.AUDIO).startWith(activityResultLauncher)
    }

    /**
     * Starts the process for handling image/video capture. Can open a dialog
     */
    fun openCamera(activity: Activity,
                   vectorPreferences: VectorPreferences,
                   cameraActivityResultLauncher: ActivityResultLauncher<Intent>,
                   cameraVideoActivityResultLauncher: ActivityResultLauncher<Intent>) {
        PhotoOrVideoDialog(activity, vectorPreferences).show(object : PhotoOrVideoDialog.PhotoOrVideoDialogListener {
            override fun takePhoto() {
                captureUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(context, cameraActivityResultLauncher)
            }

            override fun takeVideo() {
                captureUri = MultiPicker.get(MultiPicker.CAMERA_VIDEO).startWithExpectingFile(context, cameraVideoActivityResultLauncher)
            }
        })
    }

    /**
     * Starts the process for handling contact picking
     */
    fun selectContact(activityResultLauncher: ActivityResultLauncher<Intent>) {
        MultiPicker.get(MultiPicker.CONTACT).startWith(activityResultLauncher)
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

    /**
     * This methods aims to handle share intent.
     *
     * @return true if it can handle the intent data, false otherwise
     */
    fun handleShareIntent(context: Context, intent: Intent): Boolean {
        val type = intent.resolveType(context) ?: return false
        if (type.startsWith("image")) {
            callback.onContentAttachmentsReady(
                    MultiPicker.get(MultiPicker.IMAGE).getIncomingFiles(context, intent).map {
                        it.toContentAttachmentData()
                    }
            )
        } else if (type.startsWith("video")) {
            callback.onContentAttachmentsReady(
                    MultiPicker.get(MultiPicker.VIDEO).getIncomingFiles(context, intent).map {
                        it.toContentAttachmentData()
                    }
            )
        } else if (type.startsWith("audio")) {
            callback.onContentAttachmentsReady(
                    MultiPicker.get(MultiPicker.AUDIO).getIncomingFiles(context, intent).map {
                        it.toContentAttachmentData()
                    }
            )
        } else if (type.startsWith("application") || type.startsWith("file") || type.startsWith("*")) {
            callback.onContentAttachmentsReady(
                    MultiPicker.get(MultiPicker.FILE).getIncomingFiles(context, intent).map {
                        it.toContentAttachmentData()
                    }
            )
        } else {
            return false
        }
        return true
    }
}
