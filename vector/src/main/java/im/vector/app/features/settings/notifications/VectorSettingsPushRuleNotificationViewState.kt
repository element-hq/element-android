/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule

data class VectorSettingsPushRuleNotificationViewState(
        val isLoading: Boolean = false,
        val allRules: List<PushRule> = emptyList(),
        val rulesOnError: Set<String> = emptySet(),
) : MavericksState
