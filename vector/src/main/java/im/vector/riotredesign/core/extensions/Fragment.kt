/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.extensions

import androidx.fragment.app.Fragment

fun Fragment.addFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { add(frameId, fragment) }
}

fun Fragment.replaceFragment(fragment: Fragment, frameId: Int) {
    fragmentManager?.inTransaction { replace(frameId, fragment) }
}

fun Fragment.addFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    fragmentManager?.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}

fun Fragment.addChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { add(frameId, fragment) }
}

fun Fragment.replaceChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun Fragment.addChildFragmentToBackstack(fragment: Fragment, frameId: Int, tag: String? = null) {
    childFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(tag) }
}