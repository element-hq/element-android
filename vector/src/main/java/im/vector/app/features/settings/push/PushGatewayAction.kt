/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.push

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.pushers.Pusher

sealed class PushGatewayAction : VectorViewModelAction {
    object Refresh : PushGatewayAction()
    data class RemovePusher(val pusher: Pusher) : PushGatewayAction()
}
