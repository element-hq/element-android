/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync.model.accountdata

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

/*
"m.widgets":{
   "stickerpicker_@rxl881:matrix.org_1514573757015":{
      "content":{
         "creatorUserId":"@rxl881:matrix.org",
         "data":{
            "..."
         },
         "id":"stickerpicker_@rxl881:matrix.org_1514573757015",
         "name":"Stickerpicker",
         "type":"m.stickerpicker",
         "url":"https://...",
         "waitForIframeLoad":true
      },
      "sender":"@rxl881:matrix.org"
      "state_key":"stickerpicker_@rxl881:matrix.org_1514573757015",
      "type":"m.widget"
   },
{
      "..."
   }
}
 */
@JsonClass(generateAdapter = true)
internal data class UserAccountDataWidgets(
        @Json(name = "type") override val type: String = TYPE_WIDGETS,
        @Json(name = "content") val content: Map<String, Event>
) : UserAccountData()
