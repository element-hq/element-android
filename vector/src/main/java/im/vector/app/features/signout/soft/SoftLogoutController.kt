/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.signout.soft

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.login.LoginMode
import im.vector.app.features.signout.soft.epoxy.loginCenterButtonItem
import im.vector.app.features.signout.soft.epoxy.loginErrorWithRetryItem
import im.vector.app.features.signout.soft.epoxy.loginHeaderItem
import im.vector.app.features.signout.soft.epoxy.loginPasswordFormItem
import im.vector.app.features.signout.soft.epoxy.loginRedButtonItem
import im.vector.app.features.signout.soft.epoxy.loginTextItem
import im.vector.app.features.signout.soft.epoxy.loginTitleItem
import im.vector.app.features.signout.soft.epoxy.loginTitleSmallItem
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class SoftLogoutController @Inject constructor(
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: SoftLogoutViewState? = null

    fun update(viewState: SoftLogoutViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val safeViewState = viewState ?: return
        if (safeViewState.hasUnsavedKeys is Incomplete) return

        buildHeader(safeViewState)
        buildForm(safeViewState)
        buildClearDataSection()
    }

    private fun buildHeader(state: SoftLogoutViewState) {
        val host = this
        loginHeaderItem {
            id("header")
        }
        loginTitleItem {
            id("title")
            text(host.stringProvider.getString(CommonStrings.soft_logout_title))
        }
        loginTitleSmallItem {
            id("signTitle")
            text(host.stringProvider.getString(CommonStrings.soft_logout_signin_title))
        }
        loginTextItem {
            id("signText1")
            text(
                    host.stringProvider.getString(
                            CommonStrings.soft_logout_signin_notice,
                            state.homeServerUrl.toReducedUrl(),
                            state.userDisplayName,
                            state.userId
                    )
            )
        }
        if (state.hasUnsavedKeys().orFalse()) {
            loginTextItem {
                id("signText2")
                text(host.stringProvider.getString(CommonStrings.soft_logout_signin_e2e_warning_notice))
            }
        }
    }

    private fun buildForm(state: SoftLogoutViewState) = when (state.asyncHomeServerLoginFlowRequest) {
        is Fail -> buildLoginErrorWithRetryItem(state.asyncHomeServerLoginFlowRequest.error)
        is Success -> buildLoginSuccessItem(state)
        is Loading, Uninitialized -> buildLoadingItem()
        is Incomplete -> Unit
    }

    private fun buildLoadingItem() {
        loadingItem {
            id("loading")
        }
    }

    private fun buildLoginErrorWithRetryItem(error: Throwable) {
        val host = this
        loginErrorWithRetryItem {
            id("errorRetry")
            text(host.errorFormatter.toHumanReadable(error))
            listener { host.listener?.retry() }
        }
    }

    private fun buildLoginSuccessItem(state: SoftLogoutViewState) = when (state.asyncHomeServerLoginFlowRequest.invoke()) {
        LoginMode.Password -> buildLoginPasswordForm(state)
        is LoginMode.Sso -> buildLoginSSOForm()
        is LoginMode.SsoAndPassword -> disambiguateLoginSSOAndPasswordForm(state)
        LoginMode.Unsupported -> buildLoginUnsupportedForm()
        LoginMode.Unknown, null -> Unit // Should not happen
    }

    private fun buildLoginPasswordForm(state: SoftLogoutViewState) {
        val host = this
        loginPasswordFormItem {
            id("passwordForm")
            stringProvider(host.stringProvider)
            passwordValue(state.enteredPassword)
            submitEnabled(state.enteredPassword.isNotEmpty())
            onPasswordEdited { host.listener?.passwordEdited(it) }
            errorText((state.asyncLoginAction as? Fail)?.error?.let { host.errorFormatter.toHumanReadable(it) })
            forgetPasswordClickListener { host.listener?.forgetPasswordClicked() }
            submitClickListener { host.listener?.submit() }
        }
    }

    private fun buildLoginSSOForm() {
        val host = this
        loginCenterButtonItem {
            id("sso")
            text(host.stringProvider.getString(CommonStrings.login_signin_sso))
            listener { host.listener?.signinFallbackSubmit() }
        }
    }

    private fun disambiguateLoginSSOAndPasswordForm(state: SoftLogoutViewState) {
        when (state.loginType) {
            LoginType.PASSWORD -> buildLoginPasswordForm(state)
            LoginType.SSO -> buildLoginSSOForm()
            LoginType.DIRECT,
            LoginType.CUSTOM,
            LoginType.QR,
            LoginType.UNSUPPORTED -> buildLoginUnsupportedForm()
            LoginType.UNKNOWN -> Unit
        }
    }

    private fun buildLoginUnsupportedForm() {
        val host = this
        loginCenterButtonItem {
            id("fallback")
            text(host.stringProvider.getString(CommonStrings.login_signin))
            listener { host.listener?.signinFallbackSubmit() }
        }
    }

    private fun buildClearDataSection() {
        val host = this
        loginTitleSmallItem {
            id("clearDataTitle")
            text(host.stringProvider.getString(CommonStrings.soft_logout_clear_data_title))
        }
        loginTextItem {
            id("clearDataText")
            text(host.stringProvider.getString(CommonStrings.soft_logout_clear_data_notice))
        }
        loginRedButtonItem {
            id("clearDataSubmit")
            text(host.stringProvider.getString(CommonStrings.soft_logout_clear_data_submit))
            listener { host.listener?.clearData() }
        }
    }

    interface Listener {
        fun retry()
        fun passwordEdited(password: String)
        fun submit()
        fun signinFallbackSubmit()
        fun clearData()
        fun forgetPasswordClicked()
    }
}
