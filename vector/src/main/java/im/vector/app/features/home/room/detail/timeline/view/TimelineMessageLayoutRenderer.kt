/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.view

import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout

interface TimelineMessageLayoutRenderer {
    fun renderMessageLayout(messageLayout: TimelineMessageLayout)
}
