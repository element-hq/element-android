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
import androidx.fragment.app.Fragment
import com.kbeanie.multipicker.api.AudioPicker
import com.kbeanie.multipicker.api.CameraImagePicker
import com.kbeanie.multipicker.api.ContactPicker
import com.kbeanie.multipicker.api.FilePicker
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.VideoPicker

/**
 * Factory for creating different pickers. It allows to use with fragment or activity builders.
 */
interface PickerManagerFactory {

    fun createImagePicker(): ImagePicker

    fun createCameraImagePicker(): CameraImagePicker

    fun createVideoPicker(): VideoPicker

    fun createFilePicker(): FilePicker

    fun createAudioPicker(): AudioPicker

    fun createContactPicker(): ContactPicker

}

class ActivityPickerManagerFactory(private val activity: Activity, callback: AttachmentsHelper.Callback) : PickerManagerFactory {

    private val attachmentsPickerCallback = AttachmentsPickerCallback(callback)

    override fun createImagePicker(): ImagePicker {
        return ImagePicker(activity).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
            it.allowMultiple()
        }
    }

    override fun createCameraImagePicker(): CameraImagePicker {
        return CameraImagePicker(activity).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createVideoPicker(): VideoPicker {
        return VideoPicker(activity).also {
            it.setVideoPickerCallback(attachmentsPickerCallback)
            it.allowMultiple()
        }
    }

    override fun createFilePicker(): FilePicker {
        return FilePicker(activity).also {
            it.allowMultiple()
            it.setFilePickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createAudioPicker(): AudioPicker {
        return AudioPicker(activity).also {
            it.allowMultiple()
            it.setAudioPickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createContactPicker(): ContactPicker {
        return ContactPicker(activity).also {
            it.setContactPickerCallback(attachmentsPickerCallback)
        }
    }
}

class FragmentPickerManagerFactory(private val fragment: Fragment, callback: AttachmentsHelper.Callback) : PickerManagerFactory {

    private val attachmentsPickerCallback = AttachmentsPickerCallback(callback)

    override fun createImagePicker(): ImagePicker {
        return ImagePicker(fragment).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
            it.allowMultiple()
        }
    }

    override fun createCameraImagePicker(): CameraImagePicker {
        return CameraImagePicker(fragment).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createVideoPicker(): VideoPicker {
        return VideoPicker(fragment).also {
            it.setVideoPickerCallback(attachmentsPickerCallback)
            it.allowMultiple()
        }
    }

    override fun createFilePicker(): FilePicker {
        return FilePicker(fragment).also {
            it.allowMultiple()
            it.setFilePickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createAudioPicker(): AudioPicker {
        return AudioPicker(fragment).also {
            it.allowMultiple()
            it.setAudioPickerCallback(attachmentsPickerCallback)
        }
    }

    override fun createContactPicker(): ContactPicker {
        return ContactPicker(fragment).also {
            it.setContactPickerCallback(attachmentsPickerCallback)
        }
    }

}

