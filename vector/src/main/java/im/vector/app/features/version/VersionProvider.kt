/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.version

import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.VersionCodeProvider
import javax.inject.Inject

class VersionProvider @Inject constructor(
        private val versionCodeProvider: VersionCodeProvider,
        private val buildMeta: BuildMeta,
) {

    fun getVersion(longFormat: Boolean): String {
        var result = "${buildMeta.versionName} [${versionCodeProvider.getVersionCode()}]"

        var flavor = buildMeta.flavorShortDescription

        if (flavor.isNotBlank()) {
            flavor += "-"
        }

        val gitVersion = buildMeta.gitRevision
        val gitRevisionDate = buildMeta.gitRevisionDate

        result += if (longFormat) {
            " ($flavor$gitVersion-$gitRevisionDate)"
        } else {
            " ($flavor$gitVersion)"
        }

        return result
    }
}
