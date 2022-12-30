/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.viewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import im.vector.app.R
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewModel

class OtpActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    private var etOTP: TextInputEditText? = null

    private val viewModel: OnboardingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        etOTP = (findViewById(R.id.et_otp))

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.login).setOnClickListener {
            checkOTP()
        }
    }

    private fun checkOTP() {
        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(intent.getStringExtra("storedVerificationId")!!, etOTP?.text.toString())
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        viewModel.handle(
                                OnboardingAction.AuthenticateAction.LoginPhoneNumber(
                                        intent.getStringExtra("countryCode")!!,
                                        intent.getStringExtra("phoneNumber")!!,
                                        intent.getStringExtra("phoneNumber")!!,
                                        "12345678",
                                        intent.getStringExtra("initialDeviceName")!!
                                )
                        )

                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            etOTP?.error = getString(R.string.login_validation_code_is_not_correct)
                        }
                    }
                }
    }
}


