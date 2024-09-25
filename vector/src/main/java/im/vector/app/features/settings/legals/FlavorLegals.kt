/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.settings.legals

import android.content.Context

interface FlavorLegals {
    fun hasThirdPartyNotices(): Boolean
    fun navigateToThirdPartyNotices(context: Context)
}
