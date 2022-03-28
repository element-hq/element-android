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

package im.vector.app.features.location

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.LocationAsset
import org.matrix.android.sdk.api.session.room.model.message.LocationAssetType
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent

class LocationDataTest {
    @Test
    fun validCases() {
        parseGeo("geo:12.34,56.78;13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
        parseGeo("geo:12.34,56.78") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
        // Error is ignored in case of invalid uncertainty
        parseGeo("geo:12.34,56.78;13.5z6") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
        parseGeo("geo:12.34,56.78;13. 56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
        // Space are ignored (trim)
        parseGeo("geo: 12.34,56.78;13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
        parseGeo("geo:12.34,56.78; 13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
    }

    @Test
    fun invalidCases() {
        parseGeo("").shouldBeNull()
        parseGeo("geo").shouldBeNull()
        parseGeo("geo:").shouldBeNull()
        parseGeo("geo:12.34").shouldBeNull()
        parseGeo("geo:12.34;13.56").shouldBeNull()
        parseGeo("gea:12.34,56.78;13.56").shouldBeNull()
        parseGeo("geo:12.x34,56.78;13.56").shouldBeNull()
        parseGeo("geo:12.34,56.7y8;13.56").shouldBeNull()
        // Spaces are not ignored if inside the numbers
        parseGeo("geo:12.3 4,56.78;13.56").shouldBeNull()
        parseGeo("geo:12.34,56.7 8;13.56").shouldBeNull()
        // Or in the protocol part
        parseGeo(" geo:12.34,56.78;13.56").shouldBeNull()
        parseGeo("ge o:12.34,56.78;13.56").shouldBeNull()
        parseGeo("geo :12.34,56.78;13.56").shouldBeNull()
    }

    @Test
    fun selfLocationTest() {
        val contentWithNullAsset = MessageLocationContent(body = "", geoUri = "")
        contentWithNullAsset.isSelfLocation().shouldBeTrue()

        val contentWithNullAssetType = MessageLocationContent(body = "", geoUri = "", unstableLocationAsset = LocationAsset(type = null))
        contentWithNullAssetType.isSelfLocation().shouldBeTrue()

        val contentWithSelfAssetType = MessageLocationContent(body = "", geoUri = "", unstableLocationAsset = LocationAsset(type = LocationAssetType.SELF))
        contentWithSelfAssetType.isSelfLocation().shouldBeTrue()
    }

    @Test
    fun unstablePrefixTest() {
        val geoUri = "geo :12.34,56.78;13.56"

        val contentWithUnstablePrefixes = MessageLocationContent(body = "", geoUri = "", unstableLocationInfo = LocationInfo(geoUri = geoUri))
        contentWithUnstablePrefixes.getBestLocationInfo()?.geoUri.shouldBeEqualTo(geoUri)

        val contentWithStablePrefixes = MessageLocationContent(body = "", geoUri = "", locationInfo = LocationInfo(geoUri = geoUri))
        contentWithStablePrefixes.getBestLocationInfo()?.geoUri.shouldBeEqualTo(geoUri)
    }
}
