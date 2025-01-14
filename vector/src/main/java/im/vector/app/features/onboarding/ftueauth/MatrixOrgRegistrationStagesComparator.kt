/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import org.matrix.android.sdk.api.auth.registration.Stage

class MatrixOrgRegistrationStagesComparator : Comparator<Stage> {

    override fun compare(a: Stage, b: Stage): Int {
        return a.toPriority().compareTo(b.toPriority())
    }

    private fun Stage.toPriority() = when (this) {
        is Stage.Email -> 0
        is Stage.Msisdn -> 1
        is Stage.Terms -> 2
        is Stage.ReCaptcha -> 3
        is Stage.Other -> 4
        is Stage.Dummy -> 5
    }
}
