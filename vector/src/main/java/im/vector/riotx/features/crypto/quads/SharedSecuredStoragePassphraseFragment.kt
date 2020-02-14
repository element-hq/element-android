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

package im.vector.riotx.features.crypto.quads

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.riotx.R
import im.vector.riotx.core.extensions.showPassword
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.DebouncedClickListener
import me.gujun.android.span.span
import java.util.concurrent.TimeUnit

class SharedSecuredStoragePassphraseFragment : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_ssss_access_from_passphrase

    val sharedViewModel: SharedSecureStorageViewModel by activityViewModel()

    @BindView(R.id.ssss_restore_with_passphrase_warning_text)
    lateinit var warningText: TextView

    @BindView(R.id.ssss_restore_with_passphrase_warning_reason)
    lateinit var reasonText: TextView

    @BindView(R.id.ssss_passphrase_enter_til)
    lateinit var mPassphraseInputLayout: TextInputLayout

    @BindView(R.id.ssss_passphrase_enter_edittext)
    lateinit var mPassphraseTextEdit: EditText

    @BindView(R.id.ssss_view_show_password)
    lateinit var mPassphraseReveal: ImageView

    @BindView(R.id.ssss_passphrase_submit)
    lateinit var submitButton: Button

    @BindView(R.id.ssss_passphrase_cancel)
    lateinit var cancelButton: Button

    @OnClick(R.id.ssss_view_show_password)
    fun toggleVisibilityMode() {
        sharedViewModel.handle(SharedSecureStorageAction.TogglePasswordVisibility)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        warningText.text = span {
            span(getString(R.string.enter_secret_storage_passphrase_warning)) {
                textStyle = "bold"
            }
            +" "
            +getString(R.string.enter_secret_storage_passphrase_warning_text)
        }

        reasonText.text = getString(R.string.enter_secret_storage_passphrase_reason_verify)

        mPassphraseTextEdit.editorActionEvents()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .disposeOnDestroyView()

        mPassphraseTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        mPassphraseTextEdit.textChanges()
                .subscribe {
                    mPassphraseInputLayout.error = null
                    submitButton.isEnabled = it.isNotBlank()
                }
                .disposeOnDestroyView()

        sharedViewModel.observeViewEvents {
            when (it) {
                is SharedSecureStorageViewEvent.InlineError -> {
                    mPassphraseInputLayout.error = it.message
                }
            }
        }

        submitButton.setOnClickListener(DebouncedClickListener(
                View.OnClickListener {
                    submit()
                }
        ))

        cancelButton.setOnClickListener(DebouncedClickListener(
                View.OnClickListener {
                    sharedViewModel.handle(SharedSecureStorageAction.Cancel)
                }
        ))
    }

    fun submit() {
        val text = mPassphraseTextEdit.text.toString()
        if (text.isBlank()) return // Should not reach this point as button disabled
        submitButton.isEnabled = false
        sharedViewModel.handle(SharedSecureStorageAction.SubmitPassphrase(text))
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        val shouldBeVisible = state.passphraseVisible
        mPassphraseTextEdit.showPassword(shouldBeVisible)
        mPassphraseReveal.setImageResource(if (shouldBeVisible) R.drawable.ic_eye_closed_black else R.drawable.ic_eye_black)
    }
}
