/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.html

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock

/**
 * This class is in charge of visiting nodes and tells if we have some code nodes (inline or block).
 */
class CodeVisitor : AbstractVisitor() {

    var codeKind: Kind = Kind.NONE
        private set

    override fun visit(fencedCodeBlock: FencedCodeBlock?) {
        if (codeKind == Kind.NONE) {
            codeKind = Kind.BLOCK
        }
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock?) {
        if (codeKind == Kind.NONE) {
            codeKind = Kind.BLOCK
        }
    }

    override fun visit(code: Code?) {
        if (codeKind == Kind.NONE) {
            codeKind = Kind.INLINE
        }
    }

    enum class Kind {
        NONE,
        INLINE,
        BLOCK
    }
}
