/*
 * Copyright (c) 2022 BWI GmbH
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

import im.vector.app.test.fakes.FakeWellknownService
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class UrlMapProviderTest {

    @Test
    fun `given enabled fallback to maptiler API, when map configuration is not set, then the fallback url should be returned`() {
        val wellknownService = FakeWellknownService()
        wellknownService.givenMissingMapConfiguration()
        val urlMapProvider = UrlMapProvider(wellknownService.instance, LocationSharingConfig("", true))
        urlMapProvider.getMapStyleUrl() shouldBeEqualTo urlMapProvider.fallbackMapUrl
    }

    @Test
    fun `given enabled fallback to maptiler API, when map configuration is set, then the configurated url should be returned`() {
        val wellknownService = FakeWellknownService()
        wellknownService.givenValidMapConfiguration()
        val urlMapProvider = UrlMapProvider(wellknownService.instance, LocationSharingConfig("", true))
        urlMapProvider.getMapStyleUrl() shouldBeEqualTo wellknownService.A_MAPSTYLE_URL
    }

    @Test
    fun `given disabled fallback to maptiler API, when map configuration is set, then the configurated url should be returned`() {
        val wellknownService = FakeWellknownService()
        wellknownService.givenValidMapConfiguration()
        val urlMapProvider = UrlMapProvider(wellknownService.instance, LocationSharingConfig("", false))
        urlMapProvider.getMapStyleUrl() shouldBeEqualTo wellknownService.A_MAPSTYLE_URL
    }

    @Test
    fun `given disabled fallback to maptiler API, when map configuration is not set, then empty string should be returned`() {
        val wellknownService = FakeWellknownService()
        wellknownService.givenMissingMapConfiguration()
        val urlMapProvider = UrlMapProvider(wellknownService.instance, LocationSharingConfig("", false))
        urlMapProvider.getMapStyleUrl() shouldBeEqualTo ""
    }
}
