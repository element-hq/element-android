/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import im.vector.app.InstrumentedTest
import im.vector.app.core.utils.AssetReader
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class ExplicitTermFilterTest : InstrumentedTest {

    private val explicitTermFilter = ExplicitTermFilter(AssetReader(context()))

    @Test
    fun isValidEmptyTrue() {
        explicitTermFilter.isValid("") shouldBe true
    }

    @Test
    fun isValidTrue() {
        explicitTermFilter.isValid("Hello") shouldBe true
    }

    @Test
    fun isValidFalse() {
        explicitTermFilter.isValid("nsfw") shouldBe false
    }

    @Test
    fun isValidUpCaseFalse() {
        explicitTermFilter.isValid("Nsfw") shouldBe false
    }

    @Test
    fun isValidMultilineTrue() {
        explicitTermFilter.isValid("Hello\nWorld") shouldBe true
    }

    @Test
    fun isValidMultilineFalse() {
        explicitTermFilter.isValid("Hello\nnsfw") shouldBe false
    }

    @Test
    fun isValidMultilineFalse2() {
        explicitTermFilter.isValid("nsfw\nHello") shouldBe false
    }

    @Test
    fun isValidAnalFalse() {
        explicitTermFilter.isValid("anal") shouldBe false
    }

    @Test
    fun isValidAnal2False() {
        explicitTermFilter.isValid("There is some anal in this room") shouldBe false
    }

    @Test
    fun isValidAnalysisTrue() {
        explicitTermFilter.isValid("analysis") shouldBe true
    }

    @Test
    fun isValidAnalysis2True() {
        explicitTermFilter.isValid("There is some analysis in the room") shouldBe true
    }

    @Test
    fun isValidSpecialCharFalse() {
        explicitTermFilter.isValid("18+") shouldBe false
    }

    @Test
    fun isValidSpecialChar2False() {
        explicitTermFilter.isValid("This is a room with 18+ content") shouldBe false
    }

    @Test
    fun isValidOtherSpecialCharFalse() {
        explicitTermFilter.isValid("strap-on") shouldBe false
    }

    @Test
    fun isValidOtherSpecialChar2False() {
        explicitTermFilter.isValid("This is a room with strap-on content") shouldBe false
    }

    @Test
    fun isValid18True() {
        explicitTermFilter.isValid("18") shouldBe true
    }

    @Test
    fun isValidLastFalse() {
        explicitTermFilter.isValid("zoo") shouldBe false
    }

    @Test
    fun canSearchForFalse() {
        explicitTermFilter.canSearchFor("zoo") shouldBe false
    }

    @Test
    fun canSearchForTrue() {
        explicitTermFilter.canSearchFor("android") shouldBe true
    }
}
