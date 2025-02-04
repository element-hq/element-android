/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.terms

import im.vector.app.core.platform.VectorViewModelAction

sealed class ReviewTermsAction : VectorViewModelAction {
    data class LoadTerms(val preferredLanguageCode: String) : ReviewTermsAction()
    data class MarkTermAsAccepted(val url: String, val accepted: Boolean) : ReviewTermsAction()
    object Accept : ReviewTermsAction()
}
