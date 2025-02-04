/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
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
        parseGeo("geo:12.34,56.78;u=13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
        parseGeo("geo:12.34,56.78") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
    }

    @Test
    fun lenientCases() {
        // Error is ignored in case of invalid uncertainty
        parseGeo("geo:12.34,56.78;u=13.5z6") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
        parseGeo("geo:12.34,56.78;u=13. 56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = null)
        // Space are ignored (trim)
        parseGeo("geo: 12.34,56.78;u=13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
        parseGeo("geo:12.34,56.78; u=13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
        // missing "u=" for uncertainty is ignored
        parseGeo("geo:12.34,56.78;13.56") shouldBeEqualTo
                LocationData(latitude = 12.34, longitude = 56.78, uncertainty = 13.56)
    }

    @Test
    fun invalidCases() {
        parseGeo("").shouldBeNull()
        parseGeo("geo").shouldBeNull()
        parseGeo("geo:").shouldBeNull()
        parseGeo("geo:12.34").shouldBeNull()
        parseGeo("geo:12.34;u=13.56").shouldBeNull()
        parseGeo("gea:12.34,56.78;u=13.56").shouldBeNull()
        parseGeo("geo:12.x34,56.78;u=13.56").shouldBeNull()
        parseGeo("geo:12.34,56.7y8;u=13.56").shouldBeNull()
        // Spaces are not ignored if inside the numbers
        parseGeo("geo:12.3 4,56.78;u=13.56").shouldBeNull()
        parseGeo("geo:12.34,56.7 8;u=13.56").shouldBeNull()
        // Or in the protocol part
        parseGeo(" geo:12.34,56.78;u=13.56").shouldBeNull()
        parseGeo("ge o:12.34,56.78;u=13.56").shouldBeNull()
        parseGeo("geo :12.34,56.78;u=13.56").shouldBeNull()
    }

    @Test
    fun selfLocationTest() {
        val contentWithNullAsset = MessageLocationContent(body = "", geoUri = "")
        contentWithNullAsset.isSelfLocation().shouldBeTrue()

        val contentWithNullAssetType = MessageLocationContent(body = "", geoUri = "", unstableLocationAsset = LocationAsset(type = null))
        contentWithNullAssetType.isSelfLocation().shouldBeTrue()

        val contentWithSelfAssetType = MessageLocationContent(body = "", geoUri = "", unstableLocationAsset = LocationAsset(type = LocationAssetType.SELF))
        contentWithSelfAssetType.isSelfLocation().shouldBeTrue()

        val contentWithPinAssetType = MessageLocationContent(body = "", geoUri = "", unstableLocationAsset = LocationAsset(type = LocationAssetType.PIN))
        contentWithPinAssetType.isSelfLocation().shouldBeFalse()
    }

    @Test
    fun unstablePrefixTest() {
        val geoUri = "aGeoUri"

        val contentWithUnstablePrefixes = MessageLocationContent(body = "", geoUri = "", unstableLocationInfo = LocationInfo(geoUri = geoUri))
        contentWithUnstablePrefixes.getBestLocationInfo()?.geoUri.shouldBeEqualTo(geoUri)

        val contentWithStablePrefixes = MessageLocationContent(body = "", geoUri = "", locationInfo = LocationInfo(geoUri = geoUri))
        contentWithStablePrefixes.getBestLocationInfo()?.geoUri.shouldBeEqualTo(geoUri)
    }
}
