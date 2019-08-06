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

package im.vector.riotx.features.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import arrow.core.Try
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.configureAndStart
import im.vector.riotx.core.extensions.setTextWithColoredPart
import im.vector.riotx.core.extensions.showPassword
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import im.vector.riotx.features.disclaimer.showDisclaimerDialog
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.homeserver.ServerUrlsRepository
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_login.*
import javax.inject.Inject


class LoginActivity : VectorBaseActivity() {

    @Inject lateinit var authenticator: Authenticator
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var pushRuleTriggerListener: PushRuleTriggerListener

    private var passwordShown = false

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setupNotice()
        setupAuthButton()
        setupPasswordReveal()
        homeServerField.setText(ServerUrlsRepository.getDefaultHomeServerUrl(this))
    }

    private fun setupNotice() {
        riotx_no_registration_notice.setTextWithColoredPart(R.string.riotx_no_registration_notice, R.string.riotx_no_registration_notice_colored_part)

        riotx_no_registration_notice.setOnClickListener {
            openUrlInExternalBrowser(this@LoginActivity, "https://about.riot.im/downloads")
        }
    }

    override fun onResume() {
        super.onResume()

        showDisclaimerDialog(this)
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
        progressBar.isVisible = true
        touchArea.isVisible = true
        authenticator.authenticate(homeServerConnectionConfig, login, password, object : MatrixCallback<Session> {
            override fun onSuccess(data: Session) {
                activeSessionHolder.setActiveSession(data)
                data.configureAndStart(pushRuleTriggerListener)
                goToHome()
            }

            override fun onFailure(failure: Throwable) {
                progressBar.isVisible = false
                touchArea.isVisible = false
                Toast.makeText(this@LoginActivity, "Authenticate failure: $failure", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun buildHomeServerConnectionConfig(): Try<HomeServerConnectionConfig> {
        return Try {
            val homeServerUri = homeServerField.text?.trim().toString()
            HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(homeServerUri)
                    .build()
        }
    }

    private fun setupAuthButton() {
        Observable
                .combineLatest(
                        loginField.textChanges().map { it.trim().isNotEmpty() },
                        passwordField.textChanges().map { it.trim().isNotEmpty() },
                        homeServerField.textChanges().map { it.trim().isNotEmpty() },
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
