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
package im.vector.riotx.features.attachments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.kbeanie.multipicker.api.Picker.*
import com.kbeanie.multipicker.core.PickerManager
import com.kbeanie.multipicker.utils.IntentUtils
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.core.platform.Restorable
import timber.log.Timber

private const val CAPTURE_PATH_KEY = "CAPTURE_PATH_KEY"
private const val PENDING_TYPE_KEY = "PENDING_TYPE_KEY"

/**
 * This class helps to handle attachments by providing simple methods.
 * The process is asynchronous and you must implement [Callback] methods to get the data or a failure.
 */
class AttachmentsHelper private constructor(private val pickerManagerFactory: PickerManagerFactory) : Restorable {

    companion object {
        fun create(fragment: Fragment, callback: Callback): AttachmentsHelper {
            return AttachmentsHelper(FragmentPickerManagerFactory(fragment, callback))
        }

        fun create(activity: Activity, callback: Callback): AttachmentsHelper {
            return AttachmentsHelper(ActivityPickerManagerFactory(activity, callback))
        }
    }

    interface Callback {
        fun onContactAttachmentReady(contactAttachment: ContactAttachment) {
            Timber.v("On contact attachment ready: $contactAttachment")
        }

        fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>)
        fun onAttachmentsProcessFailed()
    }

    // Capture path allows to handle camera image picking. It must be restored if the activity gets killed.
    private var capturePath: String? = null
    // The pending type is set if we have to handle permission request. It must be restored if the activity gets killed.
    var pendingType: AttachmentTypeSelectorView.Type? = null

    private val imagePicker by lazy {
        pickerManagerFactory.createImagePicker()
    }

    private val videoPicker by lazy {
        pickerManagerFactory.createVideoPicker()
    }

    private val cameraImagePicker by lazy {
        pickerManagerFactory.createCameraImagePicker()
    }

    private val filePicker by lazy {
        pickerManagerFactory.createFilePicker()
    }

    private val audioPicker by lazy {
        pickerManagerFactory.createAudioPicker()
    }

    private val contactPicker by lazy {
        pickerManagerFactory.createContactPicker()
    }

    // Restorable

    override fun onSaveInstanceState(outState: Bundle) {
        capturePath?.also {
            outState.putString(CAPTURE_PATH_KEY, it)
        }
        pendingType?.also {
            outState.putSerializable(PENDING_TYPE_KEY, it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        capturePath = savedInstanceState?.getString(CAPTURE_PATH_KEY)
        if (capturePath != null) {
            cameraImagePicker.reinitialize(capturePath)
        }
        pendingType = savedInstanceState?.getSerializable(PENDING_TYPE_KEY) as? AttachmentTypeSelectorView.Type
    }

    // Public Methods

    /**
     * Starts the process for handling file picking
     */
    fun selectFile() {
        filePicker.pickFile()
    }

    /**
     * Starts the process for handling image picking
     */
    fun selectGallery() {
        imagePicker.pickImage()
    }

    /**
     * Starts the process for handling audio picking
     */
    fun selectAudio() {
        audioPicker.pickAudio()
    }

    /**
     * Starts the process for handling capture image picking
     */
    fun openCamera() {
        capturePath = cameraImagePicker.pickImage()
    }

    /**
     * Starts the process for handling contact picking
     */
    fun selectContact() {
        contactPicker.pickContact()
    }

    /**
     * This methods aims to handle on activity result data.
     *
     * @return true if it can handle the data, false otherwise
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            val pickerManager = getPickerManagerForRequestCode(requestCode)
            if (pickerManager != null) {
                pickerManager.submit(data)
                return true
            }
        }
        return false
    }

    /**
     * This methods aims to handle share intent.
     *
     * @return true if it can handle the intent data, false otherwise
     */
    fun handleShare(intent: Intent): Boolean {
        val type = intent.type ?: return false
        if (type.startsWith("image")) {
            imagePicker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("video")) {
            videoPicker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("audio")) {
            videoPicker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("application") || type.startsWith("file") || type.startsWith("*")) {
            filePicker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else {
            return false
        }
        return true
    }

    private fun getPickerManagerForRequestCode(requestCode: Int): PickerManager? {
        return when (requestCode) {
            PICK_IMAGE_DEVICE -> imagePicker
            PICK_IMAGE_CAMERA -> cameraImagePicker
            PICK_FILE         -> filePicker
            PICK_CONTACT      -> contactPicker
            PICK_AUDIO        -> audioPicker
            else              -> null
        }
    }

}