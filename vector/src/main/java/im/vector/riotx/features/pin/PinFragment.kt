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

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.beautycoder.pflockscreen.PFFLockScreenConfiguration
import com.beautycoder.pflockscreen.fragments.PFLockScreenFragment
import im.vector.riotx.R
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.MainActivityArgs
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import javax.inject.Inject

@Parcelize
data class PinArgs(
        val pinMode: PinMode
) : Parcelable

class PinFragment @Inject constructor(
        private val pinCodeStore: PinCodeStore
) : VectorBaseFragment() {

    private val fragmentArgs: PinArgs by args()

    override fun getLayoutResId() = R.layout.fragment_pin

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (fragmentArgs.pinMode) {
            PinMode.CREATE -> showCreateFragment()
            PinMode.DELETE -> showDeleteFragment()
            PinMode.AUTH   -> showAuthFragment()
        }
    }

    private fun showDeleteFragment() {
        val encodedPin = pinCodeStore.getEncodedPin() ?: return
        val authFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(requireContext())
                .setUseFingerprint(true)
                .setTitle(getString(R.string.auth_pin_confirm_to_disable_title))
                .setClearCodeOnError(true)
                .setMode(PFFLockScreenConfiguration.MODE_AUTH)
        authFragment.setConfiguration(builder.build())
        authFragment.setEncodedPinCode(encodedPin)
        authFragment.setLoginListener(object : PFLockScreenFragment.OnPFLockScreenLoginListener {
            override fun onPinLoginFailed() {
            }

            override fun onFingerprintSuccessful() {
                lifecycleScope.launch {
                    pinCodeStore.deleteEncodedPin()
                    vectorBaseActivity.setResult(Activity.RESULT_OK)
                    vectorBaseActivity.finish()
                }
            }

            override fun onFingerprintLoginFailed() {
            }

            override fun onCodeInputSuccessful() {
                lifecycleScope.launch {
                    pinCodeStore.deleteEncodedPin()
                    vectorBaseActivity.setResult(Activity.RESULT_OK)
                    vectorBaseActivity.finish()
                }
            }
        })
        replaceFragment(R.id.pinFragmentContainer, authFragment)
    }

    private fun showCreateFragment() {
        val createFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(requireContext())
                .setNewCodeValidation(true)
                .setTitle(getString(R.string.create_pin_title))
                .setNewCodeValidationTitle(getString(R.string.create_pin_confirm_title))
                .setMode(PFFLockScreenConfiguration.MODE_CREATE)

        createFragment.setConfiguration(builder.build())
        createFragment.setCodeCreateListener(object : PFLockScreenFragment.OnPFLockScreenCodeCreateListener {
            override fun onNewCodeValidationFailed() {
                Toast.makeText(requireContext(), getString(R.string.create_pin_confirm_failure), Toast.LENGTH_SHORT).show()
            }

            override fun onCodeCreated(encodedCode: String) {
                lifecycleScope.launch {
                    pinCodeStore.storeEncodedPin(encodedCode)
                    vectorBaseActivity.setResult(Activity.RESULT_OK)
                    vectorBaseActivity.finish()
                }
            }
        })
        replaceFragment(R.id.pinFragmentContainer, createFragment)
    }

    private fun showAuthFragment() {
        val encodedPin = pinCodeStore.getEncodedPin() ?: return
        val authFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(requireContext())
                .setUseFingerprint(true)
                .setTitle(getString(R.string.auth_pin_title))
                .setLeftButton(getString(R.string.auth_pin_forgot))
                .setClearCodeOnError(true)
                .setMode(PFFLockScreenConfiguration.MODE_AUTH)
        authFragment.setConfiguration(builder.build())
        authFragment.setEncodedPinCode(encodedPin)
        authFragment.setOnLeftButtonClickListener {
            displayForgotPinWarningDialog()
        }
        authFragment.setLoginListener(object : PFLockScreenFragment.OnPFLockScreenLoginListener {
            override fun onPinLoginFailed() {
            }

            override fun onFingerprintSuccessful() {
                vectorBaseActivity.setResult(Activity.RESULT_OK)
                vectorBaseActivity.finish()
            }

            override fun onFingerprintLoginFailed() {
            }

            override fun onCodeInputSuccessful() {
                vectorBaseActivity.setResult(Activity.RESULT_OK)
                vectorBaseActivity.finish()
            }
        })
        replaceFragment(R.id.pinFragmentContainer, authFragment)
    }

    private fun displayForgotPinWarningDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.auth_pin_reset_title))
                .setMessage(getString(R.string.auth_pin_reset_content))
                .setPositiveButton(getString(R.string.auth_pin_new_pin_action)) { _, _ ->
                    launchResetPinFlow()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun launchResetPinFlow() {
        MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCredentials = true))
    }
}
