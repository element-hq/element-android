/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app

import android.content.Context
import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import im.vector.app.features.settings.legals.FlavourLegals
import javax.inject.Inject

class GoogleFlavorLegals @Inject constructor() : FlavourLegals {

    override fun hasThirdPartyNotices() = true

    override fun navigateToThirdPartyNotices(context: Context) {
        // See https://developers.google.com/android/guides/opensource
        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
    }
}
