/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.test

import com.airbnb.mvrx.MvRxState
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import io.reactivex.observers.TestObserver
import org.amshove.kluent.shouldBeEqualTo

fun String.trimIndentOneLine() = trimIndent().replace("\n", "")

fun <S : MvRxState, VA : VectorViewModelAction, VE : VectorViewEvents> VectorViewModel<S, VA, VE>.test(): ViewModelTest<S, VE> {
    val state = { com.airbnb.mvrx.withState(this) { it } }
    val viewEvents = viewEvents.observe().test()
    return ViewModelTest(state, viewEvents)
}

class ViewModelTest<S, VE>(
        val state: () -> S,
        val viewEvents: TestObserver<VE>
) {

    fun assertEvents(vararg expected: VE) {
        viewEvents.assertValues(*expected)
    }

    fun assertState(expected: S) {
        state() shouldBeEqualTo expected
    }
}
