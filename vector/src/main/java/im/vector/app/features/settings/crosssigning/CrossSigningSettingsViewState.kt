/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.crosssigning

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo

data class CrossSigningSettingsViewState(
        val crossSigningInfo: MXCrossSigningInfo? = null,
        val xSigningIsEnableInAccount: Boolean = false,
        val xSigningKeysAreTrusted: Boolean = false,
        val xSigningKeyCanSign: Boolean = true
) : MavericksState
