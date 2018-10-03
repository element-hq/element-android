package im.vector.riotredesign.features.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import im.vector.matrix.android.thread.MainThreadExecutor
import im.vector.matrix.core.api.Matrix
import im.vector.matrix.core.api.MatrixCallback
import im.vector.matrix.core.api.MatrixOptions
import im.vector.matrix.core.api.failure.Failure
import im.vector.matrix.core.api.login.data.Credentials
import im.vector.matrix.core.api.login.data.HomeServerConnectionConfig
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.features.home.HomeActivity
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : RiotActivity() {

    private val matrixOptions = MatrixOptions(mainExecutor = MainThreadExecutor())
    private val matrix = Matrix(matrixOptions)
    private val homeServerConnectionConfig = HomeServerConnectionConfig("https://matrix.org/")
    private val session = matrix.createSession(homeServerConnectionConfig)
    private val authenticator = session.authenticator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        authenticateButton.setOnClickListener { authenticate() }
    }

    private fun authenticate() {
        val login = loginField.text.trim().toString()
        val password = passwordField.text.trim().toString()
        progressBar.visibility = View.VISIBLE
        authenticator.authenticate(login, password, object : MatrixCallback<Credentials> {
            override fun onSuccess(data: Credentials?) {
                goToHomeScreen()
            }

            override fun onFailure(failure: Failure) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Authenticate failure: $failure", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun goToHomeScreen() {
        val intent = HomeActivity.newIntent(this)
        startActivity(intent)
        finish()
    }

}
