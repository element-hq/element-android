/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.analytics.plan.UserProperties.FtueUseCaseSelection

fun aUserProperties(
        ftueUseCase: FtueUseCaseSelection? = FtueUseCaseSelection.Skip
) = UserProperties(
        ftueUseCaseSelection = ftueUseCase
)
