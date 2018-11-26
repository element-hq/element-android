package org.matrix.android.authentication

import android.accounts.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

class MatrixAccountAuthenticator(var context: Context?) : AbstractAccountAuthenticator(context) {

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse,
                            accountType: String,
                            authTokenType: String?,
                            requiredFeatures: Array<String>?,
                            options: Bundle?): Bundle? {
        // Ask the user to login to an existing account or to create a new account
        // TODO options could contain "device_id" and "initial_device_display_name"
        // @see https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-login
        // @see https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-register
        val intent = Intent(context, MatrixAccountAuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account,
                                    options: Bundle): Bundle? {
        // Maybe this could be used for other endpoints that need user interactive authentication
        // @see https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-account-password
        Timber.v("confirmCredentials()")
        return null
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
        Timber.v("editProperties()")
        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account,
                              authTokenType: String, loginOptions: Bundle): Bundle {
        Timber.v("getAuthToken()")

        // If the caller requested an authToken type we don't support, then return an error
        if (authTokenType != MatrixAccountAuthenticatorActivity.AUTH_TOKEN_TYPE) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            return result
        }

        // Extract the username and auth token from the Account Manager
        val am = AccountManager.get(context)
        val authToken = am.peekAuthToken(account, authTokenType)
        if (!authToken.isNullOrBlank()) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        // If we get here, then there is no cached auth token. Since we don't store the user's 
        // password, we need to re-prompt for their credentials. We do that by creating an intent to
        // display our AuthenticatorActivity panel.
        // TODO loginOptions could contain "device_id" and "initial_device_display_name"
        // @see https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-login
        val intent = Intent(context, MatrixAccountAuthenticatorActivity::class.java)
        intent.putExtra(MatrixAccountAuthenticatorActivity.PARAM_USERNAME, account.name)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        // @see https://matrix.org/docs/spec/client_server/r0.4.0.html#client-authentication
        Timber.v("getAuthTokenLabel()")
        return "access_token"
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account,
                             features: Array<String>): Bundle {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        Timber.v("hasFeatures()")
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return result
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account,
                                   authTokenType: String, loginOptions: Bundle): Bundle? {
        Timber.v("updateCredentials()")
        return null
    }
}
