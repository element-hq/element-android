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
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapEnterPassphraseBinding
import im.vector.app.features.settings.VectorLocale
import io.reactivex.android.schedulers.AndroidSchedulers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BootstrapEnterPassphraseFragment @Inject constructor()
    : VectorBaseFragment<FragmentBootstrapEnterPassphraseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapEnterPassphraseBinding {
        return FragmentBootstrapEnterPassphraseBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.bootstrapDescriptionText.text = getString(R.string.set_a_security_phrase_notice)
        views.ssssPassphraseEnterEdittext.hint = getString(R.string.set_a_security_phrase_hint)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            views.ssssPassphraseEnterEdittext.setText(it.passphrase ?: "")
        }
        views.ssssPassphraseEnterEdittext.editorActionEvents()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .disposeOnDestroyView()

        views.ssssPassphraseEnterEdittext.textChanges()
                .subscribe {
                    // ssss_passphrase_enter_til.error = null
                    sharedViewModel.handle(BootstrapActions.UpdateCandidatePassphrase(it?.toString() ?: ""))
//                    ssss_passphrase_submit.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

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
        if (state.step !is BootstrapStep.SetupPassphrase) {
            return@withState
        }
        val score = state.passphraseStrength.invoke()?.score
        val passphrase = views.ssssPassphraseEnterEdittext.text?.toString()
        if (passphrase.isNullOrBlank()) {
            views.ssssPassphraseEnterTil.error = getString(R.string.passphrase_empty_error_message)
        } else if (score != 4) {
            views.ssssPassphraseEnterTil.error = getString(R.string.passphrase_passphrase_too_weak)
        } else {
            sharedViewModel.handle(BootstrapActions.GoToConfirmPassphrase(passphrase))
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step is BootstrapStep.SetupPassphrase) {
            state.passphraseStrength.invoke()?.let { strength ->
                val score = strength.score
                views.ssssPassphraseSecurityProgress.strength = score
                if (score in 1..3) {
                    val hint =
                            strength.feedback?.getWarning(VectorLocale.applicationLocale)?.takeIf { it.isNotBlank() }
                                    ?: strength.feedback?.getSuggestions(VectorLocale.applicationLocale)?.firstOrNull()
                    if (hint != null && hint != views.ssssPassphraseEnterTil.error.toString()) {
                        views.ssssPassphraseEnterTil.error = hint
                    }
                } else {
                    views.ssssPassphraseEnterTil.error = null
                }
            }
        }
    }
}
