/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.identity

import org.matrix.android.sdk.api.failure.Failure

sealed class IdentityServiceError : Failure.FeatureFailure() {
    object OutdatedIdentityServer : IdentityServiceError()
    object OutdatedHomeServer : IdentityServiceError()
    object NoIdentityServerConfigured : IdentityServiceError()
    object TermsNotSignedException : IdentityServiceError()
    object BulkLookupSha256NotSupported : IdentityServiceError()
    object UserConsentNotProvided : IdentityServiceError()
    object BindingError : IdentityServiceError()
    object NoCurrentBindingError : IdentityServiceError()
}
