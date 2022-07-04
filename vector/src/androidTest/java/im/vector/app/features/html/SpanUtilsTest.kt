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

package im.vector.app.features.html

import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.emoji2.text.EmojiCompat
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.InstrumentedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@Ignore
class SpanUtilsTest : InstrumentedTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            EmojiCompat.init(InstrumentationRegistry.getInstrumentation().targetContext)
        }
    }

    private val spanUtils = SpanUtils {
        val emojiCompat = EmojiCompat.get()
        emojiCompat.waitForInit()
        emojiCompat.process(it) ?: it
    }

    private fun SpanUtils.canUseTextFuture(message: CharSequence): Boolean {
        return getBindingOptions(message).canUseTextFuture
    }

    @Test
    fun canUseTextFutureString() {
        spanUtils.canUseTextFuture("test").shouldBeTrue()
    }

    @Test
    fun canUseTextFutureCharSequenceOK() {
        spanUtils.canUseTextFuture(SpannableStringBuilder().append("hello")).shouldBeTrue()
    }

    @Test
    fun canUseTextFutureCharSequenceWithSpanOK() {
        val string = SpannableString("Text with strikethrough, underline, red spans")
        string.setSpan(ForegroundColorSpan(Color.RED), 36, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spanUtils.canUseTextFuture(string) shouldBeEqualTo true
    }

    @Test
    fun canUseTextFutureCharSequenceWithSpanKOStrikethroughSpan() {
        val string = SpannableString("Text with strikethrough, underline, red spans")
        string.setSpan(StrikethroughSpan(), 10, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spanUtils.canUseTextFuture(string) shouldBeEqualTo trueIfAlwaysAllowed()
    }

    @Test
    fun canUseTextFutureCharSequenceWithSpanKOUnderlineSpan() {
        val string = SpannableString("Text with strikethrough, underline, red spans")
        string.setSpan(UnderlineSpan(), 25, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spanUtils.canUseTextFuture(string) shouldBeEqualTo trueIfAlwaysAllowed()
    }

    @Test
    fun canUseTextFutureCharSequenceWithSpanKOBoth() {
        val string = SpannableString("Text with strikethrough, underline, red spans")
        string.setSpan(StrikethroughSpan(), 10, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        string.setSpan(UnderlineSpan(), 25, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spanUtils.canUseTextFuture(string) shouldBeEqualTo trueIfAlwaysAllowed()
    }

    @Test
    fun canUseTextFutureCharSequenceWithSpanKOAll() {
        val string = SpannableString("Text with strikethrough, underline, red spans")
        string.setSpan(StrikethroughSpan(), 10, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        string.setSpan(UnderlineSpan(), 25, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        string.setSpan(ForegroundColorSpan(Color.RED), 36, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spanUtils.canUseTextFuture(string) shouldBeEqualTo trueIfAlwaysAllowed()
    }

    @Test
    fun testGetBindingOptionsRegular() {
        val string = SpannableString("Text")
        val result = spanUtils.getBindingOptions(string)
        result.canUseTextFuture shouldBeEqualTo true
    }

    @Test
    fun testGetBindingOptionsStrikethrough() {
        val string = SpannableString("Text with strikethrough")
        string.setSpan(StrikethroughSpan(), 10, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val result = spanUtils.getBindingOptions(string)
        result.canUseTextFuture shouldBeEqualTo false
    }

    @Test
    fun testGetBindingOptionsMetricAffectingSpan() {
        val string = SpannableString("Emoji \uD83D\uDE2E\u200D\uD83D\uDCA8")
        val result = spanUtils.getBindingOptions(string)
        result.canUseTextFuture shouldBeEqualTo false
    }

    private fun trueIfAlwaysAllowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.P

    private fun EmojiCompat.waitForInit() {
        val latch = CountDownLatch(1)
        registerInitCallback(object : EmojiCompat.InitCallback() {
            override fun onInitialized() = latch.countDown()
            override fun onFailed(throwable: Throwable?) {
                latch.countDown()
                throw RuntimeException(throwable)
            }
        })
        EmojiCompat.init(context())
        latch.await(30, TimeUnit.SECONDS)
    }
}
