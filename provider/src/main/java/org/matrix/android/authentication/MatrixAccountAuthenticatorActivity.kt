package org.matrix.android.authentication

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import kotlinx.android.synthetic.main.login_activity.*
import org.koin.android.ext.android.inject
import org.matrix.android.provider.R
import timber.log.Timber
import java.net.URL

/**
 * This Activity implements the user interactive authentication, currently only for login.
 * @see https://matrix.org/docs/spec/client_server/r0.4.0.html#user-interactive-authentication-api
 * Currently only m.login.password
 * @TODO:
 * m.login.recaptcha
 * m.login.oauth2
 * m.login.email.identity
 * m.login.token
 * m.login.dummy
 */
class MatrixAccountAuthenticatorActivity : AccountAuthenticatorActivity() {
    companion object {
        /** The Intent extra to store username. */
        const val PARAM_USERNAME = "m.id.user"
        /** The Intent extra to store home server. */
        const val PARAM_HOME_SERVER = "home_server"

        const val ACCOUNT_TYPE = "org.matrix.android.account"
        const val AUTH_TOKEN_TYPE = "org.matrix.android.auth_token"
    }

    private lateinit var mAccountManager: AccountManager
    private val matrix by inject<Matrix>()
    private val authenticator = matrix.authenticator()

    private var mUsername : String? = null
    private var mPassword : String = ""
    private var mRequestNewAccount: Boolean = false

    override fun onCreate(icicle: Bundle?) {
        Timber.i( "onCreate($icicle)")
        super.onCreate(icicle)

        mAccountManager = AccountManager.get(this)
        mUsername = intent.getStringExtra(PARAM_USERNAME)
        val homeServer = intent.getStringExtra(PARAM_HOME_SERVER)
        mRequestNewAccount = mUsername.isNullOrBlank()
        Timber.i( "    request new: %s", mRequestNewAccount)

        setContentView(R.layout.login_activity)
        if (!mRequestNewAccount) {
            username_edit.setText(mUsername)
            username_edit.isEnabled = false
            homeserver_edit.setText(homeServer)
        }
        message.text = getMessage()
    }

    /**
     * Handles onClick event on the Submit button. Sends username/password to
     * the server for authentication. The button is configured to call
     * handleLogin() in the layout XML.
     */
    @Suppress("UNUSED_PARAMETER")
    fun handleLogin(view: View) {
        mUsername = username_edit.text.toString().trim()
        val login = mUsername
        val password = password_edit.text.toString().trim()
        val homeserver = homeserver_edit.text.toString().trim()
        // TODO Check if login is a full matrix user id, otherwise append home server
        if (login.isNullOrBlank() || password.isNullOrBlank()) {
            message.text = getMessage()
            return
        }
        progressBar.visibility = View.VISIBLE
        val homeServerConnectionConfig = HomeServerConnectionConfig.Builder()
                .withHomeServerUri(homeserver)
                .withIdentityServerUri("https://vector.im")
                .withAntiVirusServerUri("https://matrix.org/")
                .build()
        authenticator.authenticate(homeServerConnectionConfig, login, password,
                object : MatrixCallback<Session> {
                    override fun onSuccess(data: Session) {
                        onAuthenticationResult(data.sessionParams.credentials.accessToken)
                    }

                    override fun onFailure(failure: Throwable) {
                        progressBar.visibility = View.GONE
                        onAuthenticationResult(null)
                    }
                })
    }

    /**
     * Called when the authentication process completes.
     *
     * @param authToken the authentication token returned by the server, or NULL if
     * authentication failed.
     */
    private fun onAuthenticationResult(authToken: String?) {
        Timber.i("onAuthenticationResult(${!authToken.isNullOrBlank()})")

        if (authToken.isNullOrBlank()) {
            Timber.e("onAuthenticationResult: failed to authenticate")
            if (mRequestNewAccount) {
                message.text = getText(R.string.login_activity_loginfail_text_both)
            } else {
                message.text = getText(R.string.login_activity_loginfail_text_pwonly)
            }
        } else {
            val intent = Intent()
            Timber.i("finishLogin()")
            val homeserver = URL(homeserver_edit.text.toString().trim()).host
            val account = Account("@$mUsername:$homeserver", ACCOUNT_TYPE)
            if (mRequestNewAccount) {
                val userData = Bundle()
                userData.putString(PARAM_HOME_SERVER, homeserver)
                mAccountManager.addAccountExplicitly(account, null, userData)
            }
            mAccountManager.setAuthToken(account, AUTH_TOKEN_TYPE, authToken)
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername)
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE)
            setAccountAuthenticatorResult(intent.extras)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    /**
     * Returns the message to be displayed at the top of the login dialog box.
     */
    private fun getMessage(): CharSequence? {
        return when {
            mUsername.isNullOrBlank() ->
                // If no username, then we ask the user to log in using an appropriate service.
                getText(R.string.login_activity_newaccount_text)
            mPassword.isNullOrBlank() ->
                // We have an account but no password
                getText(R.string.login_activity_loginfail_text_pwmissing)
            else -> null
        }
    }
}
