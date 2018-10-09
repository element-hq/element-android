/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy.rest.model;

import com.google.gson.annotations.SerializedName;

/**
 * Content of a m.server_notice.usage_limit_reached type event
 */
public class ServerNoticeUsageLimitContent {

    private static final String EVENT_TYPE_SERVER_NOTICE_USAGE_LIMIT = "m.server_notice.usage_limit_reached";

    // The kind of user limit, generally is monthly_active_user
    public String limit;
    @SerializedName("admin_contact")
    public String adminUri;
    @SerializedName("server_notice_type")
    public String type;

    public boolean isServerNoticeUsageLimit() {
        return EVENT_TYPE_SERVER_NOTICE_USAGE_LIMIT.equals(type);
    }

}
