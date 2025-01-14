/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui.fallbackprompt

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.databinding.FragmentBiometricDialogContainerBinding
import im.vector.app.databinding.ViewBiometricDialogContentBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * A fragment to be displayed on devices that have issues with [BiometricPrompt].
 */
class FallbackBiometricDialogFragment : DialogFragment(R.layout.fragment_biometric_dialog_container) {

    var onDismiss: (() -> Unit)? = null

    var authenticationFlow: Flow<Boolean>? = null

    private var binding: ViewBiometricDialogContentBinding? = null

    private val parsedArgs by args<Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        retainInstance = true

        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentBiometricDialogContainerBinding.bind(view).apply {
            parsedArgs.cancelActionText?.let { cancelButton.text = it }
        }

        val content = view.findViewById<ViewGroup>(R.id.dialogContent).getChildAt(0)
        binding = ViewBiometricDialogContentBinding.bind(content).apply {
            parsedArgs.description?.let { fingerprintDescription.text = it }
        }

        requireDialog().setTitle(parsedArgs.title ?: getString(im.vector.lib.ui.styles.R.string.lockscreen_sign_in))
    }

    override fun onResume() {
        super.onResume()

        val authFlow = authenticationFlow ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            authFlow.catch {
                dismiss()
            }.collect { success ->
                if (success) {
                    renderSuccess()
                } else {
                    renderFailure()
                }
            }
        }
    }

    private fun renderSuccess() {
        val contentBinding = binding ?: return
        contentBinding.fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_success_lockscreen)
        contentBinding.fingerprintStatus.apply {
            setTextColor(ResourcesCompat.getColor(resources, im.vector.lib.ui.styles.R.color.lockscreen_success_color, null))
            setText(im.vector.lib.ui.styles.R.string.lockscreen_fingerprint_success)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(200L)
            dismiss()
        }
    }

    private fun renderFailure() {
        val contentBinding = binding ?: return
        contentBinding.fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_error_lockscreen)
        contentBinding.fingerprintStatus.apply {
            setTextColor(ResourcesCompat.getColor(resources, im.vector.lib.ui.styles.R.color.lockscreen_warning_color, null))
            setText(im.vector.lib.ui.styles.R.string.lockscreen_fingerprint_not_recognized)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500L)
            resetState()
        }
    }

    private fun resetState() {
        val contentBinding = binding ?: return
        contentBinding.fingerprintIcon.setImageResource(R.drawable.lockscreen_fingerprint_40)
        contentBinding.fingerprintStatus.apply {
            setTextColor(ResourcesCompat.getColor(resources, im.vector.lib.ui.styles.R.color.lockscreen_hint_color, null))
            setText(im.vector.lib.ui.styles.R.string.lockscreen_fingerprint_hint)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismiss?.invoke()
    }

    @Parcelize
    data class Args(
            val title: String? = null,
            val description: String? = null,
            val cancelActionText: String? = null,
    ) : Parcelable

    companion object {
        fun instantiate(
                title: String? = null,
                description: String? = null,
                cancelActionText: String? = null,
        ): FallbackBiometricDialogFragment {
            return FallbackBiometricDialogFragment().also {
                val args = Args(title, description, cancelActionText)
                it.arguments = bundleOf(Mavericks.KEY_ARG to args)
            }
        }
    }
}
