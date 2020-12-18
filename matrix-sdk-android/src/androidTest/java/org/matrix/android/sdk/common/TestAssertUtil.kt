/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail

/**
 * Compare two lists and their content
 */
fun assertListEquals(list1: List<Any>?, list2: List<Any>?) {
    if (list1 == null) {
        assertNull(list2)
    } else {
        assertNotNull(list2)

        assertEquals("List sizes must match", list1.size, list2!!.size)

        for (i in list1.indices) {
            assertEquals("Elements at index $i are not equal", list1[i], list2[i])
        }
    }
}

/**
 * Compare two maps and their content
 */
fun assertDictEquals(dict1: Map<String, Any>?, dict2: Map<String, Any>?) {
    if (dict1 == null) {
        assertNull(dict2)
    } else {
        assertNotNull(dict2)

        assertEquals("Map sizes must match", dict1.size, dict2!!.size)

        for (i in dict1.keys) {
            assertEquals("Values for key $i are not equal", dict1[i], dict2[i])
        }
    }
}

/**
 * Compare two byte arrays content.
 * Note that if the arrays have not the same size, it also fails.
 */
fun assertByteArrayNotEqual(a1: ByteArray, a2: ByteArray) {
    if (a1.size != a2.size) {
        fail("Arrays have not the same size.")
    }

    for (index in a1.indices) {
        if (a1[index] != a2[index]) {
            // Difference found!
            return
        }
    }

    fail("Arrays are equals.")
}
