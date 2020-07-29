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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.beautycoder.pflockscreen.PFFLockScreenConfiguration
import com.beautycoder.pflockscreen.fragments.PFLockScreenFragment
import im.vector.riotx.R
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.VectorBaseFragment
import timber.log.Timber
import javax.inject.Inject

class PinFragment @Inject constructor(
        private val pinCodeStore: PinCodeStore,
        private val viewModelFactory: PinViewModel.Factory
) : VectorBaseFragment(), PinViewModel.Factory by viewModelFactory {

    private val viewModel: PinViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_pin

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val encodedPinCode = pinCodeStore.getEncodedPin()
        if (encodedPinCode.isNullOrBlank()) {
            showCreateFragment()
        } else {
            showAuthFragment(encodedPinCode)
        }
    }

    private fun showCreateFragment() {
        val createFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(requireContext())
                .setNewCodeValidation(true)
                .setTitle("Choose a PIN for security")
                .setNewCodeValidationTitle("Confirm PIN")
                .setMode(PFFLockScreenConfiguration.MODE_CREATE)

        createFragment.setConfiguration(builder.build())
        createFragment.setCodeCreateListener(object : PFLockScreenFragment.OnPFLockScreenCodeCreateListener {
            override fun onNewCodeValidationFailed() {
                Toast.makeText(requireContext(), "Failed to validate pin, please tap a new one.", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeCreated(encodedCode: String) {
                pinCodeStore.storeEncodedPin(encodedCode)
                vectorBaseActivity.finish()
            }
        })
        replaceFragment(R.id.pinFragmentContainer, createFragment)
    }

    private fun showAuthFragment(encodedCode: String) {
        val authFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(requireContext())
                .setUseFingerprint(true)
                .setTitle("Enter your PIN")
                .setLeftButton("Forgot PIN?")
                .setClearCodeOnError(true)
                .setMode(PFFLockScreenConfiguration.MODE_AUTH)
        authFragment.setConfiguration(builder.build())
        authFragment.setEncodedPinCode(encodedCode)
        authFragment.setOnLeftButtonClickListener {
            displayForgotPinWarningDialog()
        }
        authFragment.setLoginListener(object : PFLockScreenFragment.OnPFLockScreenLoginListener {
            override fun onPinLoginFailed() {
            }

            override fun onFingerprintSuccessful() {
                Toast.makeText(requireContext(), "Pin successful", Toast.LENGTH_LONG).show()
                vectorBaseActivity.finish()
            }

            override fun onFingerprintLoginFailed() {
            }

            override fun onCodeInputSuccessful() {
                Toast.makeText(requireContext(), "Pin successful", Toast.LENGTH_LONG).show()
                vectorBaseActivity.finish()
            }
        })
        replaceFragment(R.id.pinFragmentContainer, authFragment)
    }

    private fun displayForgotPinWarningDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle("Reset pin")
                .setMessage("To reset your PIN, you'll need to re-login and create a new one.")
                .setPositiveButton("Reset pin") { _, _ ->
                    pinCodeStore.deleteEncodedPin()
                    vectorBaseActivity.finish()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear your view, unsubscribe...
    }

    override fun invalidate() = withState(viewModel) { state ->
        Timber.v("Invalidate $state")
    }
}
