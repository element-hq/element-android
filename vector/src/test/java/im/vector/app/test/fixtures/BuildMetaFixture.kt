/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.core.resources.BuildMeta

fun aBuildMeta() = BuildMeta(
        isDebug = false,
        applicationId = "im.vector",
        lowPrivacyLoggingEnabled = false,
        versionName = "app-version-name",
        gitRevision = "abcdef",
        gitRevisionDate = "01-01-01",
        gitBranchName = "a-branch-name",
        buildNumber = "100",
        flavorDescription = "Gplay",
        flavorShortDescription = "",
)
