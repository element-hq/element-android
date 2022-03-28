/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

class SocialLoginButtonsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {

    fun interface InteractionListener {
        fun onProviderSelected(id: String?)
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

    var listener: InteractionListener? = null

    private fun update() {
        val cachedViews = emptyMap<String, MaterialButton>().toMutableMap()
        children.filterIsInstance<MaterialButton>().forEach {
            cachedViews[it.getTag(R.id.loginSignupSigninSocialLoginButtons)?.toString() ?: ""] = it
        }
        removeAllViews()
        if (ssoIdentityProviders.isNullOrEmpty()) {
            // Put a default sign in with sso button
            MaterialButton(context, null, R.attr.materialButtonOutlinedStyle).apply {
                transformationMethod = null
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }.let {
                it.text = getButtonTitle(context.getString(R.string.login_social_sso))
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
                        SsoIdentityProvider.BRAND_GOOGLE   -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_google_style)
                        }
                        SsoIdentityProvider.BRAND_GITHUB   -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_github_style)
                        }
                        SsoIdentityProvider.BRAND_APPLE    -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_apple_style)
                        }
                        SsoIdentityProvider.BRAND_FACEBOOK -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_facebook_style)
                        }
                        SsoIdentityProvider.BRAND_TWITTER  -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_twitter_style)
                        }
                        SsoIdentityProvider.BRAND_GITLAB  -> {
                            MaterialButton(context, null, R.attr.vctr_social_login_button_gitlab_style)
                        }
                        else                            -> {
                            // TODO Use iconUrl
                            MaterialButton(context, null, R.attr.materialButtonOutlinedStyle).apply {
                                transformationMethod = null
                                textAlignment = View.TEXT_ALIGNMENT_CENTER
                            }
                        }
                    }
            button.text = getButtonTitle(identityProvider.name)
            button.setTag(R.id.loginSignupSigninSocialLoginButtons, identityProvider.id)
            button.setOnClickListener {
                listener?.onProviderSelected(identityProvider.id)
            }
            addView(button)
        }
    }

    private fun getButtonTitle(providerName: String?): String {
        return when (mode) {
            Mode.MODE_SIGN_IN  -> context.getString(R.string.login_social_signin_with, providerName)
            Mode.MODE_SIGN_UP  -> context.getString(R.string.login_social_signup_with, providerName)
            Mode.MODE_CONTINUE -> context.getString(R.string.login_social_continue_with, providerName)
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
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.SocialLoginButtonsView, 0, 0)
        val modeAttr = typedArray.getInt(R.styleable.SocialLoginButtonsView_signMode, 2)
        mode = when (modeAttr) {
            0    -> Mode.MODE_SIGN_IN
            1    -> Mode.MODE_SIGN_UP
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
