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

import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback
import com.kbeanie.multipicker.api.callbacks.ContactPickerCallback
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.callbacks.VideoPickerCallback
import com.kbeanie.multipicker.api.entity.ChosenAudio
import com.kbeanie.multipicker.api.entity.ChosenFile
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.kbeanie.multipicker.api.entity.ChosenVideo

/**
 * This class delegates the PickerManager callbacks to an [AttachmentsHelper.Callback]
 */
class AttachmentsPickerCallback(private val callback: AttachmentsHelper.Callback) : ImagePickerCallback, FilePickerCallback, VideoPickerCallback, AudioPickerCallback {

    override fun onAudiosChosen(audios: MutableList<ChosenAudio>?) {
        if (audios.isNullOrEmpty()) {
            callback.onAttachmentsProcessFailed()
        } else {
            val attachments = audios.map {
                it.toContentAttachmentData()
            }
            callback.onAttachmentsReady(attachments)
        }
    }

    override fun onFilesChosen(files: MutableList<ChosenFile>?) {
        if (files.isNullOrEmpty()) {
            callback.onAttachmentsProcessFailed()
        } else {
            val attachments = files.map {
                it.toContentAttachmentData()
            }
            callback.onAttachmentsReady(attachments)
        }
    }

    override fun onImagesChosen(images: MutableList<ChosenImage>?) {
        if (images.isNullOrEmpty()) {
            callback.onAttachmentsProcessFailed()
        } else {
            val attachments = images.map {
                it.toContentAttachmentData()
            }
            callback.onAttachmentsReady(attachments)
        }
    }

    override fun onVideosChosen(videos: MutableList<ChosenVideo>?) {
        if (videos.isNullOrEmpty()) {
            callback.onAttachmentsProcessFailed()
        } else {
            val attachments = videos.map {
                it.toContentAttachmentData()
            }
            callback.onAttachmentsReady(attachments)
        }
    }

    override fun onError(error: String?) {
        callback.onAttachmentsProcessFailed()
    }

}