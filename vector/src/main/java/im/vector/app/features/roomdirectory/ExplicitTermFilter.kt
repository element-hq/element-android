/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomdirectory

import im.vector.app.core.utils.AssetReader
import javax.inject.Inject

class ExplicitTermFilter @Inject constructor(
        assetReader: AssetReader
) {
    // List of forbidden terms is in file asset forbidden_terms.txt, in lower case
    private val explicitTerms = assetReader.readAssetFile("forbidden_terms.txt")
            .orEmpty()
            .split("\n")
            .map { it.trim() }
            .distinct()
            .filter { it.isNotEmpty() }

    private val explicitContentRegex = explicitTerms
            .joinToString(prefix = ".*\\b(", separator = "|", postfix = ")\\b.*")
            .toRegex(RegexOption.IGNORE_CASE)

    fun canSearchFor(term: String): Boolean {
        return term !in explicitTerms  && term != "18+"
    }

    fun isValid(str: String): Boolean {
        return explicitContentRegex.matches(str.replace("\n", " ")).not() &&
                // Special treatment for "18+" since word boundaries does not work here
                str.contains("18+").not()
    }
}
