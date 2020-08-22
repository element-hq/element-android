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
import androidx.fragment.app.Fragment
import im.vector.app.core.platform.Restorable
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
    fun selectFile(fragment: Fragment) {
        MultiPicker.get(MultiPicker.FILE).startWith(fragment)
    }

    /**
     * Starts the process for handling image picking
     */
    fun selectGallery(fragment: Fragment) {
        MultiPicker.get(MultiPicker.IMAGE).startWith(fragment)
    }

    /**
     * Starts the process for handling audio picking
     */
    fun selectAudio(fragment: Fragment) {
        MultiPicker.get(MultiPicker.AUDIO).startWith(fragment)
    }

    /**
     * Starts the process for handling capture image picking
     */
    fun openCamera(fragment: Fragment) {
        captureUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(fragment)
    }

    /**
     * Starts the process for handling contact picking
     */
    fun selectContact(fragment: Fragment) {
        MultiPicker.get(MultiPicker.CONTACT).startWith(fragment)
    }

    /**
     * This methods aims to handle on activity result data.
     *
     * @return true if it can handle the data, false otherwise
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                MultiPicker.REQUEST_CODE_PICK_FILE    -> {
                    callback.onContentAttachmentsReady(
                            MultiPicker.get(MultiPicker.FILE)
                                    .getSelectedFiles(context, requestCode, resultCode, data)
                                    .map { it.toContentAttachmentData() }
                    )
                }
                MultiPicker.REQUEST_CODE_PICK_AUDIO   -> {
                    callback.onContentAttachmentsReady(
                            MultiPicker.get(MultiPicker.AUDIO)
                                    .getSelectedFiles(context, requestCode, resultCode, data)
                                    .map { it.toContentAttachmentData() }
                    )
                }
                MultiPicker.REQUEST_CODE_PICK_CONTACT -> {
                    MultiPicker.get(MultiPicker.CONTACT)
                            .getSelectedFiles(context, requestCode, resultCode, data)
                            .firstOrNull()
                            ?.toContactAttachment()
                            ?.let {
                                callback.onContactAttachmentReady(it)
                            }
                }
                MultiPicker.REQUEST_CODE_PICK_IMAGE   -> {
                    callback.onContentAttachmentsReady(
                            MultiPicker.get(MultiPicker.IMAGE)
                                    .getSelectedFiles(context, requestCode, resultCode, data)
                                    .map { it.toContentAttachmentData() }
                    )
                }
                MultiPicker.REQUEST_CODE_TAKE_PHOTO   -> {
                    captureUri?.let { captureUri ->
                        MultiPicker.get(MultiPicker.CAMERA)
                                .getTakenPhoto(context, requestCode, resultCode, captureUri)
                                ?.let {
                                    callback.onContentAttachmentsReady(
                                            listOf(it).map { it.toContentAttachmentData() }
                                    )
                                }
                    }
                }
                MultiPicker.REQUEST_CODE_PICK_VIDEO   -> {
                    callback.onContentAttachmentsReady(
                            MultiPicker.get(MultiPicker.VIDEO)
                                    .getSelectedFiles(context, requestCode, resultCode, data)
                                    .map { it.toContentAttachmentData() }
                    )
                }
                else                                  -> return false
            }
            return true
        }
        return false
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
