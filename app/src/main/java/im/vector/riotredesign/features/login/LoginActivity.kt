package im.vector.riotredesign.features.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.features.home.HomeActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.android.ext.android.inject

class LoginActivity : RiotActivity() {

    private val matrix by inject<Matrix>()
    private val authenticator = matrix.authenticator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        authenticateButton.setOnClickListener { authenticate() }
    }

    private fun authenticate() {
        val login = loginField.text.trim().toString()
        val password = passwordField.text.trim().toString()
        progressBar.visibility = View.VISIBLE
        val homeServerConnectionConfig = HomeServerConnectionConfig.Builder()
                .withHomeServerUri("https://matrix.org/")
                .withIdentityServerUri("https://vector.im")
                .withAntiVirusServerUri("https://matrix.org/")
                .build()

        authenticator.authenticate(homeServerConnectionConfig, login, password, object : MatrixCallback<Session> {
            override fun onSuccess(data: Session) {
                matrix.currentSession = data
                matrix.currentSession.open()
                goToHome()
            }

            override fun onFailure(failure: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Authenticate failure: $failure", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun goToHome() {
        val intent = HomeActivity.newIntent(this)
        startActivity(intent)
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }

    }

}
