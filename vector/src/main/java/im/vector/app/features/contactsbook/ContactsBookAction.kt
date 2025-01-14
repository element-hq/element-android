/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.contactsbook

import im.vector.app.core.platform.VectorViewModelAction

sealed class ContactsBookAction : VectorViewModelAction {
    data class FilterWith(val filter: String) : ContactsBookAction()
    data class OnlyBoundContacts(val onlyBoundContacts: Boolean) : ContactsBookAction()
    object UserConsentRequest : ContactsBookAction()
    object UserConsentGranted : ContactsBookAction()
}
