/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.auth.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class SsoIdentityProvider(
        /**
         * The id field would be opaque with the accepted characters matching unreserved URI characters as defined in RFC3986
         * - this was chosen to avoid having to encode special characters in the URL. Max length 128.
         */
        @Json(name = "id") val id: String,
        /**
         * The name field should be the human readable string intended for printing by the client.
         */
        @Json(name = "name") val name: String?,
        /**
         * The icon field is the only optional field and should point to an icon representing the IdP.
         * If present then it must be an HTTPS URL to an image resource.
         * This should be hosted by the homeserver service provider to not leak the client's IP address unnecessarily.
         */
        @Json(name = "icon") val iconUrl: String?
) : Parcelable {

    companion object {
        // Not really defined by the spec, but we may define some ids here
        const val ID_GOOGLE = "google"
        const val ID_GITHUB = "github"
        const val ID_APPLE = "apple"
        const val ID_FACEBOOK = "facebook"
        const val ID_TWITTER = "twitter"
    }
}
