/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapEnterPassphraseBinding
import im.vector.app.features.settings.VectorLocaleProvider
import im.vector.lib.core.utils.flow.throttleFirst
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@AndroidEntryPoint
class BootstrapEnterPassphraseFragment :
        VectorBaseFragment<FragmentBootstrapEnterPassphraseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapEnterPassphraseBinding {
        return FragmentBootstrapEnterPassphraseBinding.inflate(inflater, container, false)
    }

    @Inject lateinit var vectorLocale: VectorLocaleProvider

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.bootstrapDescriptionText.text = getString(CommonStrings.set_a_security_phrase_notice)
        views.ssssPassphraseEnterEdittext.hint = getString(CommonStrings.set_a_security_phrase_hint)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            views.ssssPassphraseEnterEdittext.setText(it.passphrase ?: "")
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
                    // ssss_passphrase_enter_til.error = null
                    sharedViewModel.handle(BootstrapActions.UpdateCandidatePassphrase(it.toString()))
//                    ssss_passphrase_submit.isEnabled = it.isNotBlank()
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
        if (state.step !is BootstrapStep.SetupPassphrase) {
            return@withState
        }
        val score = state.passphraseStrength.invoke()?.score
        val passphrase = views.ssssPassphraseEnterEdittext.text?.toString()
        if (passphrase.isNullOrBlank()) {
            views.ssssPassphraseEnterTil.error = getString(CommonStrings.passphrase_empty_error_message)
        } else if (score != 4) {
            views.ssssPassphraseEnterTil.error = getString(CommonStrings.passphrase_passphrase_too_weak)
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
                            strength.feedback?.getWarning(vectorLocale.applicationLocale)?.takeIf { it.isNotBlank() }
                                    ?: strength.feedback?.getSuggestions(vectorLocale.applicationLocale)?.firstOrNull()
                    if (hint != null && hint != views.ssssPassphraseEnterTil.error.toString()) {
                        views.ssssPassphraseEnterTil.error = hint
                    }
                } else {
                    views.ssssPassphraseEnterTil.error = null
                }
            }
        }
        views.bootstrapDescriptionText.giveAccessibilityFocusOnce()
    }
}
