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

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.toMultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType

class BuiltInCameraPicker {

    /**
     * Start camera by using a ActivityResultLauncher.
     * @return Uri of taken photo or null if the operation is cancelled.
     */
    fun start(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>, targetClass: Class<*>) {
        val intent = Intent(context, targetClass)
        activityResultLauncher.launch(intent)
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken photo or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenPhoto(context: Context, photoUri: Uri): MultiPickerImageType? {
        return photoUri.toMultiPickerImageType(context)
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken video or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenVideo(context: Context, videoUri: Uri): MultiPickerVideoType? {
        return videoUri.toMultiPickerVideoType(context)
    }
}
