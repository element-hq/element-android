package im.vector.riotredesign.features.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import arrow.core.Try
import com.jakewharton.rxbinding2.widget.RxTextView
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.features.home.HomeActivity
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_login.*

private const val DEFAULT_HOME_SERVER_URI = "https://matrix.org"
private const val DEFAULT_IDENTITY_SERVER_URI = "https://vector.im"
private const val DEFAULT_ANTIVIRUS_SERVER_URI = "https://matrix.org"

class LoginActivity : RiotActivity() {

    private val authenticator = Matrix.getInstance().authenticator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupAuthButton()
        homeServerField.setText(DEFAULT_HOME_SERVER_URI)
    }

    private fun authenticate() {
        val login = loginField.text?.trim().toString()
        val password = passwordField.text?.trim().toString()
        buildHomeServerConnectionConfig().fold(
                { Toast.makeText(this@LoginActivity, "Authenticate failure: $it", Toast.LENGTH_LONG).show() },
                { authenticateWith(it, login, password) }
        )
    }

    private fun authenticateWith(homeServerConnectionConfig: HomeServerConnectionConfig, login: String, password: String) {
        progressBar.visibility = View.VISIBLE
        authenticator.authenticate(homeServerConnectionConfig, login, password, object : MatrixCallback<Session> {
            override fun onSuccess(data: Session) {
                Matrix.getInstance().currentSession = data
                Matrix.getInstance().currentSession.open()
                goToHome()
            }

            override fun onFailure(failure: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Authenticate failure: $failure", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun buildHomeServerConnectionConfig(): Try<HomeServerConnectionConfig> {
        return Try {
            val homeServerUri = homeServerField.text?.trim().toString()
            HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(homeServerUri)
                    .withIdentityServerUri(DEFAULT_IDENTITY_SERVER_URI)
                    .withAntiVirusServerUri(DEFAULT_ANTIVIRUS_SERVER_URI)
                    .build()
        }
    }

    private fun setupAuthButton() {
        Observable
                .combineLatest(
                        RxTextView.textChanges(loginField).map { it.trim().isNotEmpty() },
                        RxTextView.textChanges(passwordField).map { it.trim().isNotEmpty() },
                        RxTextView.textChanges(homeServerField).map { it.trim().isNotEmpty() },
                        Function3<Boolean, Boolean, Boolean, Boolean> { isLoginNotEmpty, isPasswordNotEmpty, isHomeServerNotEmpty ->
                            isLoginNotEmpty && isPasswordNotEmpty && isHomeServerNotEmpty
                        }
                )
                .subscribeBy { authenticateButton.isEnabled = it }
                .disposeOnDestroy()
        authenticateButton.setOnClickListener { authenticate() }
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
