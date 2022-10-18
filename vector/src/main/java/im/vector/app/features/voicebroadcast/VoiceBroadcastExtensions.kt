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

package im.vector.app.features.voicebroadcast

import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent

fun MessageAudioEvent?.isVoiceBroadcast() = this?.getVoiceBroadcastEventId() != null

fun MessageAudioEvent.getVoiceBroadcastEventId(): String? =
        // TODO Improve this condition by checking the referenced event type
        root.takeIf { content.voiceMessageIndicator != null }
                ?.getRelationContent()?.takeIf { it.type == RelationType.REFERENCE }
                ?.eventId
