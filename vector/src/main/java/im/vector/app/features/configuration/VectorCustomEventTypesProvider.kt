/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.configuration

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import org.matrix.android.sdk.api.provider.CustomEventTypesProvider
import javax.inject.Inject

class VectorCustomEventTypesProvider @Inject constructor() : CustomEventTypesProvider {

    override val customPreviewableEventTypes = listOf(
            VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO
    )
}
