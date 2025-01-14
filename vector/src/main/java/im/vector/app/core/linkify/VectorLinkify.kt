/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.linkify

import android.text.Spannable
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.core.text.util.LinkifyCompat

object VectorLinkify {
    /**
     * Better support for auto link than the default implementation.
     */
    fun addLinks(spannable: Spannable, keepExistingUrlSpan: Boolean = false) {
        // we might want to modify some matches
        val createdSpans = ArrayList<LinkSpec>()

        if (keepExistingUrlSpan) {
            // Keep track of existing URLSpans, and mark them as important
            spannable.forEachUrlSpanIndexed { _, urlSpan, start, end ->
                createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start, end, important = true))
            }
        }

        // Use the framework first, the found span can then be manipulated if needed
        LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS)

        // we might want to modify some matches
        spannable.forEachUrlSpanIndexed { _, urlSpan, start, end ->
            spannable.removeSpan(urlSpan)

            // remove short PN, too much false positive
            if (urlSpan.url?.startsWith("tel:") == true) {
                if (end - start > 6) { // Do not match under 7 digit
                    createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start, end))
                }
                return@forEachUrlSpanIndexed
            }

            // include mailto: if found before match
            if (urlSpan.url?.startsWith("mailto:") == true) {
                val protocolLength = "mailto:".length
                if (start - protocolLength >= 0 && "mailto:" == spannable.substring(start - protocolLength, start)) {
                    // modify to include the protocol
                    createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start - protocolLength, end))
                } else {
                    createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start, end))
                }

                return@forEachUrlSpanIndexed
            }

            // Handle url matches

            // check trailing space
            if (end < spannable.length - 1 && spannable[end] == '/') {
                // modify the span to include the slash
                val spec = LinkSpec(URLSpan(urlSpan.url + "/"), start, end + 1)
                createdSpans.add(spec)
                return@forEachUrlSpanIndexed
            }
            // Try to do something for ending ) issues/3020
            if (spannable[end - 1] == ')') {
                var lbehind = end - 2
                var isFullyContained = 1
                while (lbehind > start) {
                    val char = spannable[lbehind]
                    if (char == '(') isFullyContained -= 1
                    if (char == ')') isFullyContained += 1
                    lbehind--
                }
                if (isFullyContained != 0) {
                    // In this case we will return false to match, and manually add span if we want?
                    val span = URLSpan(spannable.substring(start, end - 1))
                    val spec = LinkSpec(span, start, end - 1)
                    createdSpans.add(spec)
                    return@forEachUrlSpanIndexed
                }
            }

            createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start, end))
        }

        LinkifyCompat.addLinks(spannable, VectorAutoLinkPatterns.GEO_URI.toPattern(), "geo:", arrayOf("geo:"), geoMatchFilter, null)
        spannable.forEachUrlSpanIndexed { _, urlSpan, start, end ->
            spannable.removeSpan(urlSpan)
            createdSpans.add(LinkSpec(URLSpan(urlSpan.url), start, end))
        }

        pruneOverlaps(createdSpans)
        for (spec in createdSpans) {
            spannable.setSpan(spec.span, spec.start, spec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun pruneOverlaps(links: ArrayList<LinkSpec>) {
        links.sortWith(COMPARATOR)
        var len = links.size
        var i = 0
        while (i < len - 1) {
            val a = links[i]
            val b = links[i + 1]
            var remove = -1

            // test if there is an overlap
            if (b.start in a.start until a.end) {
                if (a.important != b.important) {
                    remove = if (a.important) i + 1 else i
                } else {
                    when {
                        b.end <= a.end ->
                            // b is inside a -> b should be removed
                            remove = i + 1
                        a.end - a.start > b.end - b.start ->
                            // overlap and a is bigger -> b should be removed
                            remove = i + 1
                        a.end - a.start < b.end - b.start ->
                            // overlap and a is smaller -> a should be removed
                            remove = i
                    }
                }

                if (remove != -1) {
                    links.removeAt(remove)
                    len--
                    continue
                }
            }
            i++
        }
    }

    private data class LinkSpec(
            val span: URLSpan,
            val start: Int,
            val end: Int,
            val important: Boolean = false
    )

    private val COMPARATOR = Comparator<LinkSpec> { (_, startA, endA), (_, startB, endB) ->
        if (startA < startB) {
            return@Comparator -1
        }

        if (startA > startB) {
            return@Comparator 1
        }

        if (endA < endB) {
            return@Comparator 1
        }

        if (endA > endB) {
            -1
        } else 0
    }

    // Exclude short match that don't have geo: prefix, e.g do not highlight things like 1,2
    private val geoMatchFilter = Linkify.MatchFilter { s, start, end ->
        if (s[start] != 'g') { // doesn't start with geo:
            return@MatchFilter end - start > 12
        }
        return@MatchFilter true
    }

    private inline fun Spannable.forEachUrlSpanIndexed(action: (index: Int, urlSpan: URLSpan, start: Int, end: Int) -> Unit) {
        getSpans(0, length, URLSpan::class.java)
                .forEachIndexed { index, urlSpan ->
                    val start = getSpanStart(urlSpan)
                    val end = getSpanEnd(urlSpan)
                    action.invoke(index, urlSpan, start, end)
                }
    }
}
