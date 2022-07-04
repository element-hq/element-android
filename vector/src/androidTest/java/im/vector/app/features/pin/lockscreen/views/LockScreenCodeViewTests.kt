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
