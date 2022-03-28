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

package im.vector.app.features.crypto.quads

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.startImportTextFromFileIntent
import im.vector.app.databinding.FragmentSsssAccessFromKeyBinding
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.tryOrNull
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class SharedSecuredStorageKeyFragment @Inject constructor() : VectorBaseFragment<FragmentSsssAccessFromKeyBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSsssAccessFromKeyBinding {
        return FragmentSsssAccessFromKeyBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: SharedSecureStorageViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.ssssRestoreWithKeyText.text = getString(R.string.enter_secret_storage_input_key)

        views.ssssKeyEnterEdittext.editorActionEvents()
                .throttleFirst(300)
                .onEach {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.ssssKeyEnterEdittext.textChanges()
                .skipInitialValue()
                .onEach {
                    views.ssssKeyEnterTil.error = null
                    views.ssssKeySubmit.isEnabled = it.isNotBlank()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.ssssKeyUseFile.debouncedClicks { startImportTextFromFileIntent(requireContext(), importFileStartForActivityResult) }

        views.ssssKeyReset.views.bottomSheetActionClickableZone.debouncedClicks {
            sharedViewModel.handle(SharedSecureStorageAction.ForgotResetAll)
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                is SharedSecureStorageViewEvent.KeyInlineError -> {
                    views.ssssKeyEnterTil.error = it.message
                }
                else                                           -> Unit
            }
        }

        views.ssssKeySubmit.debouncedClicks { submit() }
    }

    fun submit() {
        val text = views.ssssKeyEnterEdittext.text.toString()
        if (text.isBlank()) return // Should not reach this point as button disabled
        views.ssssKeySubmit.isEnabled = false
        sharedViewModel.handle(SharedSecureStorageAction.SubmitKey(text))
    }

    private val importFileStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.data?.let { dataURI ->
                tryOrNull {
                    activity?.contentResolver?.openInputStream(dataURI)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.let {
                                views.ssssKeyEnterEdittext.setText(it)
                            }
                }
            }
        }
    }
}
