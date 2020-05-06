/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.sync.model.accountdata

import com.squareup.moshi.Json
import im.vector.matrix.android.internal.session.user.accountdata.AccountDataContent

abstract class UserAccountData : AccountDataContent {

    @Json(name = "type") abstract val type: String

    companion object {
        const val TYPE_IGNORED_USER_LIST = "m.ignored_user_list"
        const val TYPE_DIRECT_MESSAGES = "m.direct"
        const val TYPE_BREADCRUMBS = "im.vector.setting.breadcrumbs" // Was previously "im.vector.riot.breadcrumb_rooms"
        const val TYPE_PREVIEW_URLS = "org.matrix.preview_urls"
        const val TYPE_WIDGETS = "m.widgets"
        const val TYPE_PUSH_RULES = "m.push_rules"
        const val TYPE_IDENTITY = "m.identity"
    }
}
