/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.hardware.vibrate
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLockScreenBinding
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.views.LockScreenCodeView

@AndroidEntryPoint
class LockScreenFragment :
        VectorBaseFragment<FragmentLockScreenBinding>() {

    var lockScreenListener: LockScreenListener? = null
    var onLeftButtonClickedListener: View.OnClickListener? = null

    private val viewModel: LockScreenViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLockScreenBinding =
            FragmentLockScreenBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBindings(views)

        viewModel.observeViewEvents {
            handleEvent(it)
        }

        viewModel.handle(LockScreenAction.OnUIReady)
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.pinCodeState) {
            is PinCodeState.FirstCodeEntered -> {
                setupTitleView(views.titleTextView, true, state.lockScreenConfiguration)
                lockScreenListener?.onFirstCodeEntered()
            }
            is PinCodeState.Idle -> {
                setupTitleView(views.titleTextView, false, state.lockScreenConfiguration)
            }
        }

        renderDeleteOrFingerprintButtons(views, views.codeView.enteredDigits)
    }

    private fun onAuthFailure(method: AuthMethod) {
        lockScreenListener?.onAuthenticationFailure(method)

        val configuration = withState(viewModel) { it.lockScreenConfiguration }
        if (configuration.vibrateOnError) {
            vibrate(requireContext(), 400)
        }

        if (configuration.animateOnError) {
            context?.let {
                val animation = AnimationUtils.loadAnimation(it, R.anim.lockscreen_shake_animation)
                views.codeView.startAnimation(animation)
            }
        }
    }

    private fun onAuthError(authMethod: AuthMethod, throwable: Throwable) {
        lockScreenListener?.onAuthenticationError(authMethod, throwable)
        withState(viewModel) { state ->
            if (state.lockScreenConfiguration.clearCodeOnError) {
                views.codeView.clearCode()
            }
        }
    }

    private fun handleEvent(viewEvent: LockScreenViewEvent) {
        when (viewEvent) {
            is LockScreenViewEvent.CodeCreationComplete -> lockScreenListener?.onPinCodeCreated()
            is LockScreenViewEvent.ClearPinCode -> {
                if (viewEvent.confirmationFailed) {
                    lockScreenListener?.onNewCodeValidationFailed()
                }
                views.codeView.clearCode()
            }
            is LockScreenViewEvent.AuthSuccessful -> lockScreenListener?.onAuthenticationSuccess(viewEvent.method)
            is LockScreenViewEvent.AuthFailure -> onAuthFailure(viewEvent.method)
            is LockScreenViewEvent.AuthError -> onAuthError(viewEvent.method, viewEvent.throwable)
            is LockScreenViewEvent.ShowBiometricKeyInvalidatedMessage -> lockScreenListener?.onBiometricKeyInvalidated()
            is LockScreenViewEvent.ShowBiometricPromptAutomatically -> showBiometricPrompt()
        }
    }

    private fun setupBindings(binding: FragmentLockScreenBinding) = with(binding) {
        val configuration = withState(viewModel) { it.lockScreenConfiguration }
        val lockScreenMode = configuration.mode

        configuration.title?.let { titleTextView.text = it }
        configuration.subtitle?.let {
            subtitleTextView.text = it
            subtitleTextView.isVisible = true
        }

        setupTitleView(titleTextView, false, configuration)
        setupCodeView(codeView, configuration)
        setupCodeButton('0', button0, this)
        setupCodeButton('1', button1, this)
        setupCodeButton('2', button2, this)
        setupCodeButton('3', button3, this)
        setupCodeButton('4', button4, this)
        setupCodeButton('5', button5, this)
        setupCodeButton('6', button6, this)
        setupCodeButton('7', button7, this)
        setupCodeButton('8', button8, this)
        setupCodeButton('9', button9, this)
        setupDeleteButton(buttonDelete, this)
        setupFingerprintButton(buttonFingerPrint)
        setupLeftButton(buttonLeft, lockScreenMode, configuration)
        renderDeleteOrFingerprintButtons(this, 0)
    }

    private fun setupTitleView(titleView: TextView, isConfirmation: Boolean, configuration: LockScreenConfiguration) = with(titleView) {
        text = if (isConfirmation) {
            configuration.newCodeConfirmationTitle ?: getString(im.vector.lib.ui.styles.R.string.lockscreen_confirm_pin)
        } else {
            configuration.title ?: getString(im.vector.lib.ui.styles.R.string.lockscreen_title)
        }
    }

    private fun setupCodeView(lockScreenCodeView: LockScreenCodeView, configuration: LockScreenConfiguration) = with(lockScreenCodeView) {
        codeLength = configuration.pinCodeLength
        onCodeCompleted = LockScreenCodeView.CodeCompletedListener { code ->
            viewModel.handle(LockScreenAction.PinCodeEntered(code))
        }
    }

    private fun setupCodeButton(value: Char, view: View, binding: FragmentLockScreenBinding) {
        view.setOnClickListener {
            val size = binding.codeView.onCharInput(value)
            renderDeleteOrFingerprintButtons(binding, size)
        }
    }

    private fun setupDeleteButton(view: View, binding: FragmentLockScreenBinding) {
        view.setOnClickListener {
            val size = binding.codeView.deleteLast()
            renderDeleteOrFingerprintButtons(binding, size)
        }
    }

    private fun setupFingerprintButton(view: View) {
        view.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun setupLeftButton(view: TextView, lockScreenMode: LockScreenMode, configuration: LockScreenConfiguration) = with(view) {
        isVisible = lockScreenMode == LockScreenMode.VERIFY && configuration.leftButtonVisible
        configuration.leftButtonTitle?.let { text = it }
        setOnClickListener(onLeftButtonClickedListener)
    }

    private fun renderDeleteOrFingerprintButtons(binding: FragmentLockScreenBinding, digits: Int) = withState(viewModel) { state ->
        val showFingerprintButton = state.canUseBiometricAuth && !state.isBiometricKeyInvalidated && digits == 0
        binding.buttonFingerPrint.isVisible = showFingerprintButton
        binding.buttonDelete.isVisible = !showFingerprintButton && digits > 0
    }

    private fun showBiometricPrompt() {
        viewModel.handle(LockScreenAction.ShowBiometricPrompt(requireActivity()))
    }
}
