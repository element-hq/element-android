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

package im.vector.app.features.crypto.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapEnterPassphraseBinding
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class BootstrapConfirmPassphraseFragment @Inject constructor() :
    VectorBaseFragment<FragmentBootstrapEnterPassphraseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapEnterPassphraseBinding {
        return FragmentBootstrapEnterPassphraseBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.ssssPassphraseSecurityProgress.isGone = true

        views.bootstrapDescriptionText.text = getString(R.string.set_a_security_phrase_again_notice)
        views.ssssPassphraseEnterEdittext.hint = getString(R.string.set_a_security_phrase_hint)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            views.ssssPassphraseEnterEdittext.setText(it.passphraseRepeat ?: "")
            views.ssssPassphraseEnterEdittext.requestFocus()
        }

        views.ssssPassphraseEnterEdittext.editorActionEvents()
                .throttleFirst(300)
                .onEach {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.ssssPassphraseEnterEdittext.textChanges()
                .onEach {
                    views.ssssPassphraseEnterTil.error = null
                    sharedViewModel.handle(BootstrapActions.UpdateConfirmCandidatePassphrase(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        sharedViewModel.observeViewEvents {
            //            when (it) {
//                is SharedSecureStorageViewEvent.InlineError -> {
//                    ssss_passphrase_enter_til.error = it.message
//                }
//            }
        }

        views.bootstrapSubmit.debouncedClicks { submit() }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.ConfirmPassphrase) {
            return@withState
        }
        val passphrase = views.ssssPassphraseEnterEdittext.text?.toString()
        when {
            passphrase.isNullOrBlank()     ->
                views.ssssPassphraseEnterTil.error = getString(R.string.passphrase_empty_error_message)
            passphrase != state.passphrase ->
                views.ssssPassphraseEnterTil.error = getString(R.string.passphrase_passphrase_does_not_match)
            else                           -> {
                view?.hideKeyboard()
                sharedViewModel.handle(BootstrapActions.DoInitialize(passphrase))
            }
        }
    }
}
