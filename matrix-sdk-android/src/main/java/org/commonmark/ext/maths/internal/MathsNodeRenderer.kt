/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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
package org.commonmark.ext.maths.internal

import org.commonmark.ext.maths.DisplayMaths
import org.commonmark.ext.maths.InlineMaths
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer

internal abstract class MathsNodeRenderer : NodeRenderer {
    override fun getNodeTypes(): Set<Class<out Node>> {
        val types: MutableSet<Class<out Node>> = HashSet()
        types.add(InlineMaths::class.java)
        types.add(DisplayMaths::class.java)
        return types
    }
}
