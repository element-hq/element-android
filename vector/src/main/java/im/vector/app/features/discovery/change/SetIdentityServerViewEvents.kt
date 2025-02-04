/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.discovery.change

import androidx.annotation.StringRes
import im.vector.app.core.platform.VectorViewEvents

sealed class SetIdentityServerViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : SetIdentityServerViewEvents()
    data class Failure(@StringRes val errorMessageId: Int, val forDefault: Boolean) : SetIdentityServerViewEvents()
    data class OtherFailure(val failure: Throwable) : SetIdentityServerViewEvents()

    data class ShowTerms(val identityServerUrl: String) : SetIdentityServerViewEvents()

    object NoTerms : SetIdentityServerViewEvents()
    object TermsAccepted : SetIdentityServerViewEvents()
}
