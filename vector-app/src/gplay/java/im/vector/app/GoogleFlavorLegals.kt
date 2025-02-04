/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import android.content.Context
import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import im.vector.app.features.settings.legals.FlavorLegals
import javax.inject.Inject

class GoogleFlavorLegals @Inject constructor() : FlavorLegals {

    override fun hasThirdPartyNotices() = true

    override fun navigateToThirdPartyNotices(context: Context) {
        // See https://developers.google.com/android/guides/opensource
        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
    }
}
