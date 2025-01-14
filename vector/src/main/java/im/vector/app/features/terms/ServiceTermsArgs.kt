/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.terms

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.terms.TermsService

@Parcelize
data class ServiceTermsArgs(
        val type: TermsService.ServiceType,
        val baseURL: String,
        val token: String? = null
) : Parcelable
