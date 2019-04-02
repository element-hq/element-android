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

package im.vector.riotredesign.core.dialogs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.riotredesign.R

internal sealed class DialogListItem(@DrawableRes val iconRes: Int,
                                     @StringRes val titleRes: Int) {

    object StartVoiceCall : DialogListItem(R.drawable.voice_call_green, R.string.action_voice_call)
    object StartVideoCall : DialogListItem(R.drawable.video_call_green, R.string.action_video_call)

    object SendFile : DialogListItem(R.drawable.ic_material_file, R.string.option_send_files)
    object SendVoice : DialogListItem(R.drawable.vector_micro_green, R.string.option_send_voice)
    object SendSticker : DialogListItem(R.drawable.ic_send_sticker, R.string.option_send_sticker)
    object TakePhoto : DialogListItem(R.drawable.ic_material_camera, R.string.option_take_photo)
    object TakeVideo : DialogListItem(R.drawable.ic_material_videocam, R.string.option_take_video)
    object TakePhotoVideo : DialogListItem(R.drawable.ic_material_camera, R.string.option_take_photo_video)

}
