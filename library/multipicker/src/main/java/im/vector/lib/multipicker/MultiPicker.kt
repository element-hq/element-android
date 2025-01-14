/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

class MultiPicker<T> private constructor() {

    companion object Type {
        val IMAGE by lazy { MultiPicker<ImagePicker>() }
        val MEDIA by lazy { MultiPicker<MediaPicker>() }
        val FILE by lazy { MultiPicker<FilePicker>() }
        val VIDEO by lazy { MultiPicker<VideoPicker>() }
        val AUDIO by lazy { MultiPicker<AudioPicker>() }
        val CONTACT by lazy { MultiPicker<ContactPicker>() }
        val CAMERA by lazy { MultiPicker<CameraPicker>() }
        val CAMERA_VIDEO by lazy { MultiPicker<CameraVideoPicker>() }

        @Suppress("UNCHECKED_CAST")
        fun <T> get(type: MultiPicker<T>): T {
            return when (type) {
                IMAGE -> ImagePicker() as T
                VIDEO -> VideoPicker() as T
                MEDIA -> MediaPicker() as T
                FILE -> FilePicker() as T
                AUDIO -> AudioPicker() as T
                CONTACT -> ContactPicker() as T
                CAMERA -> CameraPicker() as T
                CAMERA_VIDEO -> CameraVideoPicker() as T
                else -> throw IllegalArgumentException("Unsupported type $type")
            }
        }
    }
}
