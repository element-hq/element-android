package im.vector.app.features.firebaseauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import im.vector.app.R
import im.vector.app.core.extensions.CabinetActivity
import im.vector.app.core.extensions.content
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.ftueauth.LoginFieldsValidation
import im.vector.app.features.onboarding.ftueauth.onPasswordError
import im.vector.app.features.onboarding.ftueauth.onUsernameOrIdError
import im.vector.app.features.onboarding.ftueauth.onValid
import io.realm.Realm
import org.scilab.forge.jlatexmath.UnderscoreAtom.s
import java.util.concurrent.TimeUnit

class OtpActivity : AppCompatActivity() {
    // get reference of the firebase auth
    lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    var etOTP: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        etOTP = (findViewById(R.id.et_otp))

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.login).setOnClickListener {
            checkOTP()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            // This method is called when the verification is completed
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                finish()
//                Log.d("GFG" , "onVerificationCompleted Success")
            }

            // Called when verification is failed add log statement to see the exception
            override fun onVerificationFailed(e: FirebaseException) {
//                Log.d("GFG" , "onVerificationFailed $e")
            }

            // On code is sent by the firebase this method is called
            // in here we start a new activity where user can enter the OTP
            override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
            ) {
//                Log.d("GFG","onCodeSent: $verificationId")
                this@OtpActivity.storedVerificationId = verificationId
                resendToken = token
            }
        }

        val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
        val phoneNumber = preferences?.getString("phone_number", "")

        sendVerificationCode(phoneNumber!!)
    }

    private fun checkOTP() {
        val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, etOTP?.text.toString())
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
                        val editor = preferences?.edit()
                        editor?.putBoolean("isLoggedIn", true)
                        editor?.apply()
                        finish()
                    } else {
                        // Sign in failed, display a message and update the UI
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            etOTP?.error = getString(R.string.login_validation_code_is_not_correct)
                        }
                    }
                }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number) // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this) // Activity (for callback binding)
                .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
//        Log.d("GFG" , "Auth started")
    }
}


