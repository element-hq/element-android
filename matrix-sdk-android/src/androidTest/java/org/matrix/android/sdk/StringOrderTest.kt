/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk

import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.Test
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.util.StringOrderUtils

class StringOrderTest {

    @Test
    fun testbasing() {
        assertEquals("a", StringOrderUtils.baseToString(StringOrderUtils.stringToBase("a", StringOrderUtils.DEFAULT_ALPHABET), StringOrderUtils.DEFAULT_ALPHABET))
        assertEquals("element", StringOrderUtils.baseToString(StringOrderUtils.stringToBase("element", StringOrderUtils.DEFAULT_ALPHABET), StringOrderUtils.DEFAULT_ALPHABET))
        assertEquals("matrix", StringOrderUtils.baseToString(StringOrderUtils.stringToBase("matrix", StringOrderUtils.DEFAULT_ALPHABET), StringOrderUtils.DEFAULT_ALPHABET))
    }

    @Test
    fun testValid() {
        println(StringOrderUtils.DEFAULT_ALPHABET.joinToString(","))

        assert(MatrixPatterns.isValidOrderString("a"))
        assert(MatrixPatterns.isValidOrderString(" "))
        assert(MatrixPatterns.isValidOrderString("abc"))
        assert(!MatrixPatterns.isValidOrderString("abcê"))
        assert(!MatrixPatterns.isValidOrderString(""))
        assert(MatrixPatterns.isValidOrderString("!"))
        assert(MatrixPatterns.isValidOrderString("!\"#\$%&'()*+,012"))
        assert(!MatrixPatterns.isValidOrderString(Char(' '.code - 1).toString()))

        assert(!MatrixPatterns.isValidOrderString(
                buildString {
                    for (i in 0..49) {
                        append(StringOrderUtils.DEFAULT_ALPHABET.random())
                    }
                }
        ))

        assert(MatrixPatterns.isValidOrderString(
                buildString {
                    for (i in 0..48) {
                        append(StringOrderUtils.DEFAULT_ALPHABET.random())
                    }
                }
        ))
    }

    @Test
    fun testAverage() {
        assertAverage("${StringOrderUtils.DEFAULT_ALPHABET.first()}", "m")
        assertAverage("aa", "aab")
        assertAverage("matrix", "element")
        assertAverage("mmm", "mmmmm")
        assertAverage("aab", "aa")
        assertAverage("", "aa")
        assertAverage("a", "z")
        assertAverage("ground", "sky")
    }

    @Test
    fun testMidPoints() {
        val orders = StringOrderUtils.midPoints("element", "matrix", 4)
        assertEquals(4, orders!!.size)
        assert("element" < orders[0])
        assert(orders[0] < orders[1])
        assert(orders[1] < orders[2])
        assert(orders[2] < orders[3])
        assert(orders[3] < "matrix")

        println("element < ${orders.joinToString(" < ") { "[$it]" }} < matrix")

        val orders2 = StringOrderUtils.midPoints("a", "d", 4)
        assertEquals(null, orders2)
    }

    @Test
    fun testRenumberNeeded() {
        assertEquals(null, StringOrderUtils.average("a", "a"))
        assertEquals(null, StringOrderUtils.average("", ""))
        assertEquals(null, StringOrderUtils.average("a", "b"))
        assertEquals(null, StringOrderUtils.average("b", "a"))
        assertEquals(null, StringOrderUtils.average("mmmm", "mmmm"))
        assertEquals(null, StringOrderUtils.average("a${Char(0)}", "a"))
    }

    private fun assertAverage(first: String, second: String) {
        val left = first.takeIf { first < second } ?: second
        val right = first.takeIf { first > second } ?: second
        val av1 = StringOrderUtils.average(left, right)!!
        println("[$left] < [$av1] < [$right]")
        Assert.assertTrue(left < av1)
        Assert.assertTrue(av1 < right)
    }
}
