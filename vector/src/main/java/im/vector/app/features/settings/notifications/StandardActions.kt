/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import org.matrix.android.sdk.api.session.pushrules.Action

sealed class StandardActions(
        val actions: List<Action>?
) {
    object Notify : StandardActions(actions = listOf(Action.Notify))
    object NotifyDefaultSound : StandardActions(actions = listOf(Action.Notify, Action.Sound()))
    object NotifyRingSound : StandardActions(actions = listOf(Action.Notify, Action.Sound(sound = Action.ACTION_OBJECT_VALUE_VALUE_RING)))
    object Highlight : StandardActions(actions = listOf(Action.Notify, Action.Highlight(highlight = true)))
    object HighlightDefaultSound : StandardActions(actions = listOf(Action.Notify, Action.Highlight(highlight = true), Action.Sound()))
    object DontNotify : StandardActions(actions = emptyList())
    object Disabled : StandardActions(actions = null)
}
