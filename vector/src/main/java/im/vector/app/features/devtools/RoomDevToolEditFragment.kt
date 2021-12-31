/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentDevtoolsEditorBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class RoomDevToolEditFragment @Inject constructor() :
    VectorBaseFragment<FragmentDevtoolsEditorBinding>() {

    private val sharedViewModel: RoomDevToolViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDevtoolsEditorBinding {
        return FragmentDevtoolsEditorBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        withState(sharedViewModel) {
            views.editText.setText(it.editedContent ?: "{}")
        }
        views.editText.textChanges()
                .skipInitialValue()
                .onEach {
                    sharedViewModel.handle(RoomDevToolAction.UpdateContentText(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        views.editText.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        views.editText.hideKeyboard()
    }
}
