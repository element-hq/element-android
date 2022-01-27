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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal open class JSonViewerModel(var key: String?, var index: Int?, val jObject: Any) {
    var depth = 0
    var isExpanded = false
}

internal interface Composed {
    fun addChild(model: JSonViewerModel)
}

internal class JSonViewerObject(key: String?, index: Int?, jObject: JSONObject) :
    JSonViewerModel(key, index, jObject),
    Composed {

    var keys = LinkedHashMap<String, JSonViewerModel>()

    override fun addChild(model: JSonViewerModel) {
        keys[model.key!!] = model
    }
}

internal class JSonViewerArray(key: String?, index: Int?, jObject: JSONArray) :
    JSonViewerModel(key, index, jObject), Composed {
    var items = ArrayList<JSonViewerModel>()

    override fun addChild(model: JSonViewerModel) {
        items.add(model)
    }
}

internal class JSonViewerLeaf(key: String?, index: Int?, val stringRes: String, val type: JSONType) :
    JSonViewerModel(key, index, stringRes)

internal enum class JSONType {
    STRING,
    NUMBER,
    BOOLEAN,
    NULL
}

internal object ModelParser {

    @Throws(JSONException::class)
    fun fromJsonString(jsonString: String, initialOpenDepth: Int = -1): JSonViewerObject {
        val jobj = JSONObject(jsonString.trim())
        val root = JSonViewerObject(null, null, jobj).apply { isExpanded = true }
        jobj.keys().forEach {
            eval(root, it, null, jobj.get(it), 1, initialOpenDepth)
        }
        return root
    }

    private fun eval(parent: Composed, key: String?, index: Int?, obj: Any, depth: Int, initialOpenDepth: Int) {
        when (obj) {
            is JSONObject -> {
                val objectComposed = JSonViewerObject(key, index, obj)
                    .apply { isExpanded = initialOpenDepth == -1 || depth <= initialOpenDepth }
                objectComposed.depth = depth
                obj.keys().forEach {
                    eval(objectComposed, it, null, obj.get(it), depth + 1, initialOpenDepth)
                }
                parent.addChild(objectComposed)
            }
            is JSONArray -> {
                val objectComposed = JSonViewerArray(key, index, obj)
                    .apply { isExpanded = initialOpenDepth == -1 || depth <= initialOpenDepth }
                objectComposed.depth = depth
                for (i in 0 until obj.length()) {
                    eval(objectComposed, null, i, obj[i], depth + 1, initialOpenDepth)
                }
                parent.addChild(objectComposed)
            }
            is String -> {
                JSonViewerLeaf(key, index,  obj, JSONType.STRING).let {
                    it.depth = depth
                    parent.addChild(it)
                }
            }
            is Number -> {
                JSonViewerLeaf(key, index, obj.toString(), JSONType.NUMBER).let {
                    it.depth = depth
                    parent.addChild(it)
                }
            }
            is Boolean -> {
                JSonViewerLeaf(key, index, obj.toString(), JSONType.BOOLEAN).let {
                    it.depth = depth
                    parent.addChild(it)
                }
            }
            else -> {
                if (obj == JSONObject.NULL) {
                    JSonViewerLeaf(key, index, "null", JSONType.NULL).let {
                        it.depth = depth
                        parent.addChild(it)
                    }
                }
            }
        }
    }
}
