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

package im.vector.app.core.animations

import android.content.Context
import android.util.AttributeSet
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet

class VectorFullTransitionSet : TransitionSet {

    constructor() {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        ordering = ORDERING_TOGETHER
        addTransition(Fade(Fade.OUT))
                .addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
                .addTransition(Fade(Fade.IN))
    }
}
