/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInRange
import org.junit.Test

class WaveFormSanitizerTest {

    private val waveFormSanitizer = WaveFormSanitizer()

    @Test
    fun sanitizeNull() {
        waveFormSanitizer.sanitize(null) shouldBe null
    }

    @Test
    fun sanitizeEmpty() {
        waveFormSanitizer.sanitize(emptyList()) shouldBe null
    }

    @Test
    fun sanitizeSingleton() {
        val result = waveFormSanitizer.sanitize(listOf(1))!!
        result.size shouldBe 30
        checkResult(result)
    }

    @Test
    fun sanitize29() {
        val list = generateSequence { 1 }.take(29).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitize30() {
        val list = generateSequence { 1 }.take(30).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        result.size shouldBe 30
        checkResult(result)
    }

    @Test
    fun sanitize31() {
        val list = generateSequence { 1 }.take(31).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitize119() {
        val list = generateSequence { 1 }.take(119).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitize120() {
        val list = generateSequence { 1 }.take(120).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        result.size shouldBe 120
        checkResult(result)
    }

    @Test
    fun sanitize121() {
        val list = generateSequence { 1 }.take(121).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitize1024() {
        val list = generateSequence { 1 }.take(1024).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitizeNegative() {
        val list = generateSequence { -1 }.take(30).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitizeMaxValue() {
        val list = generateSequence { 1025 }.take(30).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    @Test
    fun sanitizeNegativeMaxValue() {
        val list = generateSequence { -1025 }.take(30).toList()
        val result = waveFormSanitizer.sanitize(list)!!
        checkResult(result)
    }

    private fun checkResult(result: List<Int>) {
        result.forEach {
            it shouldBeInRange 0..1024
        }

        result.size shouldBeInRange 30..120
    }
}
