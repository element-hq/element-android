/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import im.vector.app.core.platform.VectorViewEvents

sealed class CreatePollViewEvents : VectorViewEvents {
    object Success : CreatePollViewEvents()
    object EmptyQuestionError : CreatePollViewEvents()
    data class NotEnoughOptionsError(val requiredOptionsCount: Int) : CreatePollViewEvents()
}
