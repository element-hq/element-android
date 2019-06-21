/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.showPassword
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.features.home.HomeActivity
import im.vector.riotredesign.features.notifications.PushRuleTriggerListener
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.android.ext.android.get

private const val DEFAULT_HOME_SERVER_URI = "https://matrix.org"
private const val DEFAULT_IDENTITY_SERVER_URI = "https://vector.im"
private const val DEFAULT_ANTIVIRUS_SERVER_URI = "https://matrix.org"

class LoginActivity : VectorBaseActivity() {

    private val authenticator = Matrix.getInstance().authenticator()

    private var passwordShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupAuthButton()
        setupPasswordReveal()
        homeServerField.setText(DEFAULT_HOME_SERVER_URI)
    }

    private fun authenticate() {
        passwordShown = false
        renderPasswordField()

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
                data.open()
                data.setFilter(FilterService.FilterPreset.RiotFilter)
                data.startSync()
                get<PushRuleTriggerListener>().startWithSession(data)
                data.fetchPushRules()
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

    private fun setupPasswordReveal() {
        passwordShown = false

        passwordReveal.setOnClickListener {
            passwordShown = !passwordShown

            renderPasswordField()
        }

        renderPasswordField()
    }

    private fun renderPasswordField() {
        passwordField.showPassword(passwordShown)

        passwordReveal.setImageResource(if (passwordShown) R.drawable.ic_eye_closed_black else R.drawable.ic_eye_black)
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
