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

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.beautycoder.pflockscreen.PFFLockScreenConfiguration
import com.beautycoder.pflockscreen.fragments.PFLockScreenFragment
import com.beautycoder.pflockscreen.fragments.PFLockScreenFragment.OnPFLockScreenCodeCreateListener
import im.vector.riotx.R
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity

class PinActivity : VectorBaseActivity(), ToolbarConfigurable {

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, PinActivity::class.java)
        }
    }

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        showCreateFragment()
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    private fun showCreateFragment() {
        val createFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(this)
                .setNewCodeValidation(true)
                .setTitle("Choose a PIN for security")
                .setNewCodeValidationTitle("Confirm PIN")
                .setMode(PFFLockScreenConfiguration.MODE_CREATE)

        createFragment.setConfiguration(builder.build())
        createFragment.setCodeCreateListener(object : OnPFLockScreenCodeCreateListener {
            override fun onNewCodeValidationFailed() {
            }

            override fun onCodeCreated(encodedCode: String) {
                showAuthFragment(encodedCode)
            }
        })
        replaceFragment(R.id.simpleFragmentContainer, createFragment)
    }

    private fun showAuthFragment(encodedCode: String) {
        val authFragment = PFLockScreenFragment()
        val builder = PFFLockScreenConfiguration.Builder(this)
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
                Toast.makeText(this@PinActivity, "Pin successful", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onFingerprintLoginFailed() {
            }

            override fun onCodeInputSuccessful() {
                Toast.makeText(this@PinActivity, "Pin successful", Toast.LENGTH_LONG).show()
                finish()
            }
        })
        replaceFragment(R.id.simpleFragmentContainer, authFragment)
    }

    private fun displayForgotPinWarningDialog() {
        AlertDialog.Builder(this)
                .setTitle("Reset pin")
                .setMessage("To reset your PIN, you'll need to re-login and create a new one.")
                .setPositiveButton("Reset pin") { _, _ ->
                    showCreateFragment()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }
}
