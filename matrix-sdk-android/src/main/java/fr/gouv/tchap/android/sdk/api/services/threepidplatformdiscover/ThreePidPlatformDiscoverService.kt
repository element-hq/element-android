/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.android.sdk.api.services.threepidplatformdiscover

import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import org.matrix.android.sdk.api.session.identity.ThreePid

/**
 * Provides access to services identity server can provide
 */
interface ThreePidPlatformDiscoverService {

    /**
     * Retrieve the Tchap platform from a 3rd party id.
     * See https://github.com/matrix-org/sydent/blob/dinsic/docs/info.md
     * @param url identity server url
     * @param threePid 3rd party id
     */
    suspend fun getPlatform(url: String, threePid: ThreePid): Platform

}
