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

package im.vector.app.features.version

import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.VersionCodeProvider
import javax.inject.Inject

class VersionProvider @Inject constructor(
        private val versionCodeProvider: VersionCodeProvider,
        private val buildMeta: BuildMeta,
) {

    fun getVersion(longFormat: Boolean, useBuildNumber: Boolean): String {
        var result = "${buildMeta.versionName} [${versionCodeProvider.getVersionCode()}]"

        var flavor = buildMeta.flavorShortDescription

        if (flavor.isNotBlank()) {
            flavor += "-"
        }

        var gitVersion = buildMeta.gitRevision
        val gitRevisionDate = buildMeta.gitRevisionDate
        val buildNumber = buildMeta.buildNumber

        var useLongFormat = longFormat

        if (useBuildNumber && buildNumber != "0") {
            // It's a build from CI
            gitVersion = "b$buildNumber"
            useLongFormat = false
        }

        result += if (useLongFormat) {
            " ($flavor$gitVersion-$gitRevisionDate)"
        } else {
            " ($flavor$gitVersion)"
        }

        return result
    }
}
