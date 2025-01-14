/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.html

import android.widget.TextView
import androidx.core.text.toSpanned
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.DimensionConverter
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
        every { it.isRichTextEditorEnabled() } returns false
    }
    private val fakeSessionHolder = mockk<ActiveSessionHolder>()
    private val fakeDimensionConverter = mockk<DimensionConverter>()

    private val renderer = EventHtmlRenderer(
            MatrixHtmlPluginConfigure(ColorProvider(context), context.resources, fakeVectorPreferences, fakeDimensionConverter),
            context,
            fakeVectorPreferences,
            fakeSessionHolder,
    )

    private val textView: TextView = TextView(context)

    @Test
    fun takesInitialListPositionIntoAccount() {
        val result = """<ol start="5"><li>first entry<li></ol>""".renderAsTestSpan()

        result shouldBeEqualTo "[list item]5.${nbsp}first entry[/list item]\n"
    }

    @Test
    fun doesNotProcessMarkdownWithinCodeBlocks() {
        val result = """<code>__italic__ **bold**</code>""".renderAsTestSpan()

        result shouldBeEqualTo "[inline code]__italic__ **bold**[/inline code]"
    }

    @Test
    fun doesNotProcessMarkdownBoldAndItalic() {
        val result = """__italic__ **bold**""".renderAsTestSpan()

        result shouldBeEqualTo "__italic__ **bold**"
    }

    // https://github.com/noties/Markwon/issues/423
    @Test
    fun doesNotIntroduceExtraNewLines() {
        // Given initial render (required to trigger bug)
        """Some <i>italic</i>""".renderAsTestSpan()
        val results = arrayOf(
                """Some <i>italic</i>""".renderAsTestSpan(),
                """Some <b>bold</b>""".renderAsTestSpan(),
                """Some <code>code</code>""".renderAsTestSpan(),
                """Some <a href="link">link</a>""".renderAsTestSpan(),
                """Some <del>strikethrough</del>""".renderAsTestSpan(),
                """Some <span>span</span>""".renderAsTestSpan(),
        )

        results shouldBeEqualTo arrayOf(
                "Some [italic]italic[/italic]",
                "Some [bold]bold[/bold]",
                "Some [inline code]code[/inline code]",
                "Some [link]link[/link]",
                "Some \n[strikethrough]strikethrough[/strikethrough]", // FIXME
                "Some \nspan", // FIXME
        )
    }

    @Test
    fun processesHtmlWithinCodeBlocks() {
        val result = """<code><i>italic</i> <b>bold</b></code>""".renderAsTestSpan()

        result shouldBeEqualTo "[inline code][italic]italic[/italic] [bold]bold[/bold][/inline code]"
    }

    @Test
    fun processesHtmlWithinCodeBlocks_givenRichTextEditorEnabled() {
        every { fakeVectorPreferences.isRichTextEditorEnabled() } returns true
        val result = """<code><i>italic</i> <b>bold</b></code>""".renderAsTestSpan()

        result shouldBeEqualTo "[inline code][italic]italic[/italic] [bold]bold[/bold][/inline code]"
    }

    @Test
    fun processesHtmlEntities() {
        val result = """&amp; &lt; &gt; &apos; &quot;""".renderAsTestSpan()

        result shouldBeEqualTo """& < > ' """"
    }

    private fun String.renderAsTestSpan(): String {
        textView.text = renderer.render(this).toSpanned()
        renderer.plugins.forEach { markwonPlugin -> markwonPlugin.afterSetText(textView) }
        return textView.text.toSpanned().toTestSpan()
    }
}
