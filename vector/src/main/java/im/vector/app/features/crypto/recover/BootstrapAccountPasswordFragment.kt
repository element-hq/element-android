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
import androidx.core.text.toSpannable
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_bootstrap_enter_account_password.*
import kotlinx.android.synthetic.main.fragment_bootstrap_enter_passphrase.bootstrapDescriptionText
import kotlinx.android.synthetic.main.fragment_bootstrap_enter_passphrase.ssss_view_show_password
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BootstrapAccountPasswordFragment @Inject constructor(
        private val colorProvider: ColorProvider
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_bootstrap_enter_account_password

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recPassPhrase = getString(R.string.account_password)
        bootstrapDescriptionText.text = getString(R.string.enter_account_password, recPassPhrase)
                .toSpannable()
                .colorizeMatchingText(recPassPhrase, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))

        bootstrapAccountPasswordEditText.hint = getString(R.string.account_password)

        bootstrapAccountPasswordEditText.editorActionEvents()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .disposeOnDestroyView()

        bootstrapAccountPasswordEditText.textChanges()
                .distinctUntilChanged()
                .subscribe {
                    if (!it.isNullOrBlank()) {
                        bootstrapAccountPasswordTil.error = null
                    }
                }
                .disposeOnDestroyView()

        ssss_view_show_password.debouncedClicks { sharedViewModel.handle(BootstrapActions.TogglePasswordVisibility) }
        bootstrapPasswordButton.debouncedClicks { submit() }

        withState(sharedViewModel) { state ->
            (state.step as? BootstrapStep.AccountPassword)?.failure?.let {
                bootstrapAccountPasswordTil.error = it
            }
        }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.AccountPassword) {
            return@withState
        }
        val accountPassword = bootstrapAccountPasswordEditText.text?.toString()
        if (accountPassword.isNullOrBlank()) {
            bootstrapAccountPasswordTil.error = getString(R.string.error_empty_field_your_password)
        } else {
            view?.hideKeyboard()
            sharedViewModel.handle(BootstrapActions.ReAuth(accountPassword))
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step is BootstrapStep.AccountPassword) {
            val isPasswordVisible = state.step.isPasswordVisible
            bootstrapAccountPasswordEditText.showPassword(isPasswordVisible, updateCursor = false)
            ssss_view_show_password.setImageResource(if (isPasswordVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
        }
    }
}
