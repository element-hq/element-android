package im.vector.app.features.firebaseauth

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import im.vector.app.R
import im.vector.app.features.MainActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewModel
import io.realm.Realm
import java.util.concurrent.TimeUnit

class PhoneNumberActivity: AppCompatActivity() {
    // this stores the phone number of the user
    var number : String = ""

    // create instance of firebase auth
    lateinit var auth: FirebaseAuth

    // we will use this to match the sent otp from firebase
    lateinit var storedVerificationId:String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number)
        auth=FirebaseAuth.getInstance()

        // start verification on click of the button
        findViewById<Button>(R.id.button_otp).setOnClickListener {
            login()
        }

        // Callback function for Phone Auth
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            // This method is called when the verification is completed
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
                val usernameOrId = preferences?.getString("usernameOrId", "")
                val initialDeviceName = preferences?.getString("initialDeviceName", "")

                (OnboardingAction.AuthenticateAction.Login(usernameOrId!!, "12345678", initialDeviceName!!))
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
                storedVerificationId = verificationId
                resendToken = token
            }
        }
    }

    private fun login() {
        number = findViewById<EditText>(R.id.et_phone_number).text.trim().toString()
        // get the phone number from edit text and append the country cde with it
        if (number.isNotEmpty()){
            number = "+$number"
            sendVerificationCode(number)
        }else{
            Toast.makeText(this,"Enter mobile number", Toast.LENGTH_SHORT).show()
        }
    }

    // this method sends the verification code
    // and starts the callback of verification
    // which is implemented above in onCreate
    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number) // Phone number to verify
                .setTimeout(10L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this) // Activity (for callback binding)
                .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
//        Log.d("GFG" , "Auth started")
    }
}
