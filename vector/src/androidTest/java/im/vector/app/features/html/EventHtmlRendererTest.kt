/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.html

import androidx.core.text.toSpannable
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.toTestSpan
import im.vector.app.features.settings.VectorPreferences
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.text.Typography.nbsp

@RunWith(JUnit4::class)
class EventHtmlRendererTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fakeVectorPreferences = mockk<VectorPreferences>().also {
        every { it.latexMathsIsEnabled() } returns false
    }

    private val renderer = EventHtmlRenderer(
            MatrixHtmlPluginConfigure(ColorProvider(context), context.resources),
            context,
            fakeVectorPreferences
    )

    @Test
    fun takesInitialListPositionIntoAccount() {
        val result = """<ol start="5"><li>first entry<li></ol>""".renderAsTestSpan()

        result shouldBeEqualTo "[list item]5.${nbsp}first entry[/list item]\n"
    }

    @Test
    fun doesNotProcessMarkdownWithinCodeBlocks() {
        val result = """<code>__italic__ **bold**</code>""".renderAsTestSpan()

        result shouldBeEqualTo "[code]__italic__ **bold**[/code]"
    }

    @Test
    fun doesNotProcessMarkdownBoldAndItalic() {
        val result = """__italic__ **bold**""".renderAsTestSpan()

        result shouldBeEqualTo "__italic__ **bold**"
    }

    @Test
    fun processesHtmlWithinCodeBlocks() {
        val result = """<code><i>italic</i> <b>bold</b></code>""".renderAsTestSpan()

        result shouldBeEqualTo "[code][italic]italic[/italic] [bold]bold[/bold][/code]"
    }

    @Test
    fun processesHtmlEntities() {
        val result = """&amp; &lt; &gt; &apos; &quot;""".renderAsTestSpan()

        result shouldBeEqualTo """& < > ' """"
    }

    private fun String.renderAsTestSpan() = renderer.render(this).toSpannable().toTestSpan()
}
