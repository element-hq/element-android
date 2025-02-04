/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.views

import androidx.test.platform.app.InstrumentationRegistry
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LockScreenCodeViewTests {

    lateinit var lockScreenCodeView: LockScreenCodeView

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lockScreenCodeView = LockScreenCodeView(context).apply { codeLength = 4 }
    }

    @Test
    fun addingCharactersChangesEnteredDigits() {
        lockScreenCodeView.onCharInput('A')
        lockScreenCodeView.enteredDigits shouldBeEqualTo 1
    }

    @Test
    fun onCharInputReturnsUpdatedDigitCount() {
        val digits = lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.enteredDigits shouldBeEqualTo digits
    }

    @Test
    fun whenDigitsEqualCodeLengthCompletionCallbackIsCalled() {
        val latch = CountDownLatch(1)
        lockScreenCodeView.onCodeCompleted = LockScreenCodeView.CodeCompletedListener { latch.countDown() }

        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 4
        latch.await(1, TimeUnit.SECONDS)
    }

    @Test
    fun whenCodeIsCompletedCannotAddMoreDigits() {
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 4

        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 4
    }

    @Test
    fun whenChangingCodeLengthCodeIsReset() {
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1

        lockScreenCodeView.codeLength = 10

        lockScreenCodeView.enteredDigits shouldBeEqualTo 0
    }

    @Test
    fun changingCodeLengthToTheSameValueDoesNothing() {
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1

        lockScreenCodeView.codeLength = lockScreenCodeView.codeLength

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1
    }

    @Test
    fun clearResetsEnteredDigits() {
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1

        lockScreenCodeView.clearCode()

        lockScreenCodeView.enteredDigits shouldBeEqualTo 0
    }

    @Test
    fun deleteLastRemovesLastDigit() {
        lockScreenCodeView.onCharInput('1')
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 2

        lockScreenCodeView.deleteLast()

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1
    }

    @Test
    fun deleteLastReturnsUpdatedDigitCount() {
        lockScreenCodeView.onCharInput('1')
        val digits = lockScreenCodeView.deleteLast()
        lockScreenCodeView.enteredDigits shouldBeEqualTo digits
    }

    @Test
    fun deleteLastCannotRemoveDigitIfCodeIsEmpty() {
        lockScreenCodeView.onCharInput('1')

        lockScreenCodeView.enteredDigits shouldBeEqualTo 1

        lockScreenCodeView.deleteLast()
        lockScreenCodeView.deleteLast()

        lockScreenCodeView.enteredDigits shouldBeEqualTo 0
    }
}
