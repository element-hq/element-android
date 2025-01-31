/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class represent a localized privacy policy for registration Flow.
 */
@Parcelize
data class LocalizedFlowDataLoginTerms(
        val policyName: String?,
        val version: String?,
        val localizedUrl: String?,
        val localizedName: String?
) : Parcelable
