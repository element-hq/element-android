/*
 * Copyright 2018 New Vector Ltd
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

package org.matrix.android.sdk.common

import org.matrix.android.sdk.api.session.Session

data class CryptoTestData(val firstSession: Session,
                          val roomId: String,
                          val secondSession: Session? = null,
                          val thirdSession: Session? = null) {

    fun cleanUp(testHelper: CommonTestHelper) {
        testHelper.signOutAndClose(firstSession)
        secondSession?.let { testHelper.signOutAndClose(it) }
        thirdSession?.let { testHelper.signOutAndClose(it) }
    }
}
