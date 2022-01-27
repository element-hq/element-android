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

package org.billcarsonfr.jsonviewer

import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ModelParseTest {
    @Test
    fun parsing_isCorrect() {
        val string = """
            {
                "glossary": {
                    "title": "example glossary",
                    "GlossDiv": {
                        "title": "S",
                        "GlossList": {
                            "GlossEntry": {
                                "ID": "SGML",
                                "SortAs": "SGML",
                                "GlossTerm": "Standard Generalized Markup Language",
                                "Acronym": "SGML",
                                "Abbrev": "ISO 8879:1986",
                                "GlossDef": {
                                    "para": "A meta-markup language, used to create markup languages such as DocBook.",
                                    "GlossSeeAlso": ["GML", "XML"]
                                },
                                "GlossSee": "markup"
                            }
                        }
                    }
                }
            }
    """.trim()

        val model = ModelParser.fromJsonString(string)

        Assert.assertEquals(0, model.depth)
        Assert.assertEquals(1, model.keys.size)
        Assert.assertTrue(model.keys.containsKey("glossary"))
        Assert.assertTrue(model.keys["glossary"] is JSonViewerObject)

        val glossary = model.keys["glossary"] as JSonViewerObject
        Assert.assertEquals(2, glossary.keys.size)
        Assert.assertTrue(glossary.keys.containsKey("title"))
        Assert.assertTrue(glossary.keys.containsKey("GlossDiv"))

        Assert.assertTrue(glossary.keys["title"] is JSonViewerLeaf)
        (glossary.keys["title"] as JSonViewerLeaf).let {
            Assert.assertEquals(JSONType.STRING, it.type)
        }

        Assert.assertTrue(glossary.keys["GlossDiv"] is JSonViewerObject)
        val glossDiv = glossary.keys["GlossDiv"] as JSonViewerObject

        Assert.assertTrue(glossDiv.keys["GlossList"] is JSonViewerObject)
        val glossList = glossDiv.keys["GlossList"] as JSonViewerObject

        Assert.assertTrue(glossList.keys["GlossEntry"] is JSonViewerObject)
        val glossEntry = glossList.keys["GlossEntry"] as JSonViewerObject

        Assert.assertTrue(glossEntry.keys["GlossDef"] is JSonViewerObject)
        val glossDef = glossEntry.keys["GlossDef"] as JSonViewerObject

        Assert.assertTrue(glossDef.keys["GlossSeeAlso"] is JSonViewerArray)
        val glossSeeAlso = glossDef.keys["GlossSeeAlso"] as JSonViewerArray

        Assert.assertEquals(2, glossSeeAlso.items.size)
        Assert.assertEquals(0, glossSeeAlso.items.first().index)
        Assert.assertNull(glossSeeAlso.items.first().key)
        Assert.assertEquals("GML", (glossSeeAlso.items.first() as JSonViewerLeaf).stringRes)
    }
}
