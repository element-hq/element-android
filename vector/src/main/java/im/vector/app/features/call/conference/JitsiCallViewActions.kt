/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import im.vector.app.core.platform.VectorViewModelAction

sealed class JitsiCallViewActions : VectorViewModelAction {
    data class SwitchTo(
            val args: VectorJitsiActivity.Args,
            val withConfirmation: Boolean
    ) : JitsiCallViewActions()

    /**
     * The ViewModel will either ask the View to finish, or to join another conf.
     */
    object OnConferenceLeft : JitsiCallViewActions()
}
