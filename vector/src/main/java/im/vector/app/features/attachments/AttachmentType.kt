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

import im.vector.app.core.utils.PERMISSIONS_EMPTY
import im.vector.app.core.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import im.vector.app.core.utils.PERMISSIONS_FOR_PICKING_CONTACT
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.PERMISSIONS_FOR_VOICE_BROADCAST

/**
 * The all possible types to pick with their required permissions.
 */
enum class AttachmentType(val permissions: List<String>) {
    CAMERA(PERMISSIONS_FOR_TAKING_PHOTO),
    GALLERY(PERMISSIONS_EMPTY),
    FILE(PERMISSIONS_EMPTY),
    STICKER(PERMISSIONS_EMPTY),
    CONTACT(PERMISSIONS_FOR_PICKING_CONTACT),
    POLL(PERMISSIONS_EMPTY),
    LOCATION(PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING),
    VOICE_BROADCAST(PERMISSIONS_FOR_VOICE_BROADCAST),
}
