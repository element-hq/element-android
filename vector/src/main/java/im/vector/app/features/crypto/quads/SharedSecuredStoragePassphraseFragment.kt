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

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.text.toSpannable
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_ssss_access_from_passphrase.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SharedSecuredStoragePassphraseFragment @Inject constructor(
        private val colorProvider: ColorProvider
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_ssss_access_from_passphrase

    val sharedViewModel: SharedSecureStorageViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If has passphrase
        val pass = getString(R.string.recovery_passphrase)
        val key = getString(R.string.recovery_key)
        ssss_restore_with_passphrase_warning_text.text = getString(
                R.string.enter_secret_storage_passphrase_or_key,
                pass,
                key
        )
                .toSpannable()
                .colorizeMatchingText(pass, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))
                .colorizeMatchingText(key, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))

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
                    ssss_passphrase_enter_til.error = null
                    ssss_passphrase_submit.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

        sharedViewModel.observeViewEvents {
            when (it) {
                is SharedSecureStorageViewEvent.InlineError -> {
                    ssss_passphrase_enter_til.error = it.message
                }
            }
        }

        ssss_passphrase_submit.debouncedClicks { submit() }
        ssss_passphrase_use_key.debouncedClicks { sharedViewModel.handle(SharedSecureStorageAction.UseKey) }
        ssss_view_show_password.debouncedClicks { sharedViewModel.handle(SharedSecureStorageAction.TogglePasswordVisibility) }
    }

    fun submit() {
        val text = ssss_passphrase_enter_edittext.text.toString()
        if (text.isBlank()) return // Should not reach this point as button disabled
        ssss_passphrase_submit.isEnabled = false
        sharedViewModel.handle(SharedSecureStorageAction.SubmitPassphrase(text))
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        val shouldBeVisible = state.passphraseVisible
        ssss_passphrase_enter_edittext.showPassword(shouldBeVisible)
        ssss_view_show_password.setImageResource(if (shouldBeVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
    }
}
