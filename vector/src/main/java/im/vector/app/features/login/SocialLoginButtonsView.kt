/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import im.vector.app.R
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

class SocialLoginButtonsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        LinearLayout(context, attrs, defStyle) {

    fun interface InteractionListener {
        fun onProviderSelected(provider: SsoIdentityProvider?)
    }

    enum class Mode {
        MODE_SIGN_IN,
        MODE_SIGN_UP,
        MODE_CONTINUE,
    }

    var ssoIdentityProviders: List<SsoIdentityProvider>? = null
        set(newProviders) {
            if (newProviders != ssoIdentityProviders) {
                field = newProviders
                update()
            }
        }

    var mode: Mode = Mode.MODE_CONTINUE
        set(value) {
            if (value != mode) {
                field = value
                update()
            }
        }

    var hasOidcCompatibilityFlow: Boolean = false
        set(value) {
            if (value != hasOidcCompatibilityFlow) {
                field = value
                update()
            }
        }

    var listener: InteractionListener? = null

    private fun update() {
        val cachedViews = emptyMap<String, MaterialButton>().toMutableMap()
        children.filterIsInstance<MaterialButton>().forEach {
            cachedViews[it.getTag(R.id.loginSignupSigninSocialLoginButtons)?.toString() ?: ""] = it
        }
        removeAllViews()
        if (ssoIdentityProviders.isNullOrEmpty()) {
            // Put a default sign in with sso button
            MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                transformationMethod = null
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }.let {
                it.text = if (hasOidcCompatibilityFlow) context.getString(CommonStrings.login_continue)
                    else getButtonTitle(context.getString(CommonStrings.login_social_sso))
                it.textAlignment = View.TEXT_ALIGNMENT_CENTER
                it.setOnClickListener {
                    listener?.onProviderSelected(null)
                }
                addView(it)
            }
            return
        }

        ssoIdentityProviders?.forEach { identityProvider ->
            // Use some heuristic to render buttons according to branding guidelines
            val button: MaterialButton = cachedViews[identityProvider.id]
                    ?: when (identityProvider.brand) {
                        SsoIdentityProvider.BRAND_GOOGLE -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_google_style)
                        }
                        SsoIdentityProvider.BRAND_GITHUB -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_github_style)
                        }
                        SsoIdentityProvider.BRAND_APPLE -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_apple_style)
                        }
                        SsoIdentityProvider.BRAND_FACEBOOK -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_facebook_style)
                        }
                        SsoIdentityProvider.BRAND_TWITTER -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_twitter_style)
                        }
                        SsoIdentityProvider.BRAND_GITLAB -> {
                            MaterialButton(context, null, im.vector.lib.ui.styles.R.attr.vctr_social_login_button_gitlab_style)
                        }
                        else -> {
                            // TODO Use iconUrl
                            MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                                transformationMethod = null
                                textAlignment = View.TEXT_ALIGNMENT_CENTER
                            }
                        }
                    }
            button.text = getButtonTitle(identityProvider.name)
            button.setTag(R.id.loginSignupSigninSocialLoginButtons, identityProvider.id)
            button.setOnClickListener {
                listener?.onProviderSelected(identityProvider)
            }
            addView(button)
        }
    }

    private fun getButtonTitle(providerName: String?): String {
        return when (mode) {
            Mode.MODE_SIGN_IN -> context.getString(CommonStrings.login_social_signin_with, providerName)
            Mode.MODE_SIGN_UP -> context.getString(CommonStrings.login_social_signup_with, providerName)
            Mode.MODE_CONTINUE -> context.getString(CommonStrings.login_social_continue_with, providerName)
        }
    }

    init {
        this.orientation = VERTICAL
        gravity = Gravity.CENTER
        clipToPadding = false
        clipChildren = false
        if (isInEditMode) {
            ssoIdentityProviders = listOf(
                    SsoIdentityProvider("Google", "Google", null, SsoIdentityProvider.BRAND_GOOGLE),
                    SsoIdentityProvider("Facebook", "Facebook", null, SsoIdentityProvider.BRAND_FACEBOOK),
                    SsoIdentityProvider("Apple", "Apple", null, SsoIdentityProvider.BRAND_APPLE),
                    SsoIdentityProvider("GitHub", "GitHub", null, SsoIdentityProvider.BRAND_GITHUB),
                    SsoIdentityProvider("Twitter", "Twitter", null, SsoIdentityProvider.BRAND_TWITTER),
                    SsoIdentityProvider("Gitlab", "Gitlab", null, SsoIdentityProvider.BRAND_GITLAB),
                    SsoIdentityProvider("Custom_pro", "SSO", null, null)
            )
        }
        val typedArray = context.theme.obtainStyledAttributes(attrs, im.vector.lib.ui.styles.R.styleable.SocialLoginButtonsView, 0, 0)
        val modeAttr = typedArray.getInt(im.vector.lib.ui.styles.R.styleable.SocialLoginButtonsView_signMode, 2)
        mode = when (modeAttr) {
            0 -> Mode.MODE_SIGN_IN
            1 -> Mode.MODE_SIGN_UP
            else -> Mode.MODE_CONTINUE
        }
        typedArray.recycle()
        update()
    }

    fun dpToPx(dp: Int): Int {
        val resources = context.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}

fun SocialLoginButtonsView.render(loginMode: LoginMode, mode: SocialLoginButtonsView.Mode, listener: (SsoIdentityProvider?) -> Unit) {
    this.mode = mode
    val state = loginMode.ssoState()
    this.ssoIdentityProviders = when (state) {
        SsoState.Fallback -> null
        is SsoState.IdentityProviders -> state.providers.sorted()
    }
    this.hasOidcCompatibilityFlow = (loginMode is LoginMode.Sso && loginMode.hasOidcCompatibilityFlow) ||
            (loginMode is LoginMode.SsoAndPassword && loginMode.hasOidcCompatibilityFlow)
    this.listener = SocialLoginButtonsView.InteractionListener { listener(it) }
}
