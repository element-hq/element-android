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
import android.view.View
import android.view.inputmethod.EditorInfo
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.settings.VectorLocale
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_bootstrap_enter_passphrase.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BootstrapEnterPassphraseFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_bootstrap_enter_passphrase

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bootstrapDescriptionText.text = getString(R.string.set_a_security_phrase_notice)
        ssss_passphrase_enter_edittext.hint = getString(R.string.set_a_security_phrase_hint)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            ssss_passphrase_enter_edittext.setText(it.passphrase ?: "")
        }
        ssss_passphrase_enter_edittext.editorActionEvents()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .disposeOnDestroyView()

        ssss_passphrase_enter_edittext.textChanges()
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

        ssss_view_show_password.debouncedClicks { sharedViewModel.handle(BootstrapActions.TogglePasswordVisibility) }
        bootstrapSubmit.debouncedClicks { submit() }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.SetupPassphrase) {
            return@withState
        }
        val score = state.passphraseStrength.invoke()?.score
        val passphrase = ssss_passphrase_enter_edittext.text?.toString()
        if (passphrase.isNullOrBlank()) {
            ssss_passphrase_enter_til.error = getString(R.string.passphrase_empty_error_message)
        } else if (score != 4) {
            ssss_passphrase_enter_til.error = getString(R.string.passphrase_passphrase_too_weak)
        } else {
            sharedViewModel.handle(BootstrapActions.GoToConfirmPassphrase(passphrase))
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step is BootstrapStep.SetupPassphrase) {
            val isPasswordVisible = state.step.isPasswordVisible
            ssss_passphrase_enter_edittext.showPassword(isPasswordVisible, updateCursor = false)
            ssss_view_show_password.setImageResource(if (isPasswordVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)

            state.passphraseStrength.invoke()?.let { strength ->
                val score = strength.score
                ssss_passphrase_security_progress.strength = score
                if (score in 1..3) {
                    val hint =
                            strength.feedback?.getWarning(VectorLocale.applicationLocale)?.takeIf { it.isNotBlank() }
                                    ?: strength.feedback?.getSuggestions(VectorLocale.applicationLocale)?.firstOrNull()
                    if (hint != null && hint != ssss_passphrase_enter_til.error.toString()) {
                        ssss_passphrase_enter_til.error = hint
                    }
                } else {
                    ssss_passphrase_enter_til.error = null
                }
            }
        }
    }
}
