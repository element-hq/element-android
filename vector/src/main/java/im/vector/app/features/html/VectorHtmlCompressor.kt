/*
 * Copyright 2019 New Vector Ltd
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

import com.googlecode.htmlcompressor.compressor.Compressor
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VectorHtmlCompressor @Inject constructor() {

    // All default options are suitable so far
    private val htmlCompressor: Compressor = HtmlCompressor()

    fun compress(html: String): String {
        var result = htmlCompressor.compress(html)

        // Trim space after <br> and <p>, unfortunately the method setRemoveSurroundingSpaces() from the doc does not exist
        result = result.replace("<br> ", "<br>")
        result = result.replace("<br/> ", "<br/>")
        result = result.replace("<p> ", "<p>")

        return result
    }
}
