/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.widgets.model.Widget

data class JitsiCallViewState(
        val roomId: String = "",
        val widgetId: String = "",
        val enableVideo: Boolean = false,
        val widget: Async<Widget> = Uninitialized
) : MavericksState {

    constructor(args: VectorJitsiActivity.Args) : this(
            roomId = args.roomId,
            widgetId = args.widgetId,
            enableVideo = args.enableVideo
    )
}
