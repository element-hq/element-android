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

package im.vector.app.features.attachments

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.MultiPicker
import javax.inject.Inject

class MultiPickerIncomingFiles @Inject constructor(
        private val context: Context,
) {

    fun image(intent: Intent) = MultiPicker.get(MultiPicker.IMAGE).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun video(intent: Intent) = MultiPicker.get(MultiPicker.VIDEO).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun media(intent: Intent) = MultiPicker.get(MultiPicker.MEDIA).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun file(intent: Intent) = MultiPicker.get(MultiPicker.FILE).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun audio(intent: Intent) = MultiPicker.get(MultiPicker.AUDIO).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }
}
