/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.pin

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import timber.log.Timber
import javax.inject.Inject

class PinFragment @Inject constructor(
        private val viewModelFactory: PinViewModel.Factory
) : VectorBaseFragment(), PinViewModel.Factory by viewModelFactory {

    private val viewModel: PinViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_pin

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize your view, subscribe to viewModel...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear your view, unsubscribe...
    }

    override fun invalidate() = withState(viewModel) { state ->
        Timber.v("Invalidate $state")
    }
}
