/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings

interface VectorSettingsFragmentInteractionListener {

    fun requestHighlightPreferenceKeyOnResume(key: String?)

    fun requestedKeyToHighlight(): String?

    fun navigateToEmailAndPhoneNumbers()
}
