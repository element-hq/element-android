package im.vector.app.features.firebaseauth

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import im.vector.app.R
import io.realm.Realm
import java.util.concurrent.TimeUnit

class OtpActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private var etOTP: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        etOTP = (findViewById(R.id.et_otp))

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.login).setOnClickListener {
            checkOTP()
        }




        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                finish()
            }

            override fun onVerificationFailed(e: FirebaseException) {}

            override fun onCodeSent(
                    verificationId: String,
                    token: ForceResendingToken
            ) {
                this@OtpActivity.storedVerificationId = verificationId
                resendToken = token
            }
        }

        val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
        val phoneNumber = preferences?.getString("phone_number", "")

        findViewById<Button>(R.id.phoneConfirmationResend).setOnClickListener {

            sendVerificationCode(phoneNumber!!)

        }
//        val token = preferences?.getString("token", "")

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
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            etOTP?.error = getString(R.string.login_validation_code_is_not_correct)
                        }
                    }
                }
    }

    private fun sendVerificationCode(number: String) {
        println("xuizalupa1")
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendOTP(number: String){
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setTimeout(10L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }



//    private fun resendVerificationCode(
//            phoneNumber: String,
//            token: ForceResendingToken
//    ) {
//        PhoneAuthProvider.(
//                phoneNumber,  // Phone number to verify
//                60,  // Timeout duration
//                TimeUnit.SECONDS,  // Unit of timeout
//                this,  // Activity (for callback binding)
//                callbacks,  // OnVerificationStateChangedCallbacks
//                token
//        ) // ForceResendingToken from callbacks
//    }
}


