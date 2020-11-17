/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.lib.multipicker

class MultiPicker<T> {

    companion object Type {
        val IMAGE by lazy { MultiPicker<ImagePicker>() }
        val FILE by lazy { MultiPicker<FilePicker>() }
        val VIDEO by lazy { MultiPicker<VideoPicker>() }
        val AUDIO by lazy { MultiPicker<AudioPicker>() }
        val CONTACT by lazy { MultiPicker<ContactPicker>() }
        val CAMERA by lazy { MultiPicker<CameraPicker>() }
        val RECVIDEO by lazy { MultiPicker<RecordVideoPicker>() }

        @Suppress("UNCHECKED_CAST")
        fun <T> get(type: MultiPicker<T>): T {
            return when (type) {
                IMAGE   -> ImagePicker() as T
                VIDEO   -> VideoPicker() as T
                FILE    -> FilePicker() as T
                AUDIO   -> AudioPicker() as T
                CONTACT -> ContactPicker() as T
                CAMERA  -> CameraPicker() as T
                RECVIDEO -> RecordVideoPicker() as T
                else    -> throw IllegalArgumentException("Unsupported type $type")
            }
        }
    }
}
