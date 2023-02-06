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

package im.vector.app.features.settings.devices.v2.details.extended

import im.vector.app.features.settings.devices.v2.list.DeviceType

data class DeviceExtendedInfo(
        /**
         * One of MOBILE, WEB, DESKTOP or UNKNOWN.
         */
        val deviceType: DeviceType,
        /**
         * i.e. Google Pixel 6.
         */
        val deviceModel: String? = null,
        /**
         * i.e. Android 11.
         */
        val deviceOperatingSystem: String? = null,
        /**
         * i.e. Element Nightly.
         */
        val clientName: String? = null,
        /**
         * i.e. 1.5.0.
         */
        val clientVersion: String? = null,
)
