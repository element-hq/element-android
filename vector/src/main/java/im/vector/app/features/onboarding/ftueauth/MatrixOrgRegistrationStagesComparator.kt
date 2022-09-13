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
