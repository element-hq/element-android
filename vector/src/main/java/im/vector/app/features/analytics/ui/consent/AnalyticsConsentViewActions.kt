/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.ui.consent

import im.vector.app.core.platform.VectorViewModelAction

sealed class AnalyticsConsentViewActions : VectorViewModelAction {
    data class SetUserConsent(val userConsent: Boolean) : AnalyticsConsentViewActions()
}
