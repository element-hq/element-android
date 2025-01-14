/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.quads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@AndroidEntryPoint
class SharedSecureStorageActivity :
        SimpleFragmentActivity(),
        VectorBaseBottomSheetDialogFragment.ResultListener,
        FragmentOnAttachListener {

    @Parcelize
    data class Args(
            val keyId: String?,
            val requestedSecrets: List<String> = emptyList(),
            val resultKeyStoreAlias: String,
            val writeSecrets: List<Pair<String, String>> = emptyList(),
            val currentStep: SharedSecureStorageViewState.Step = SharedSecureStorageViewState.Step.EnterPassphrase,
    ) : Parcelable

    private val viewModel: SharedSecureStorageViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.addFragmentOnAttachListener(this)

        views.toolbar.visibility = View.GONE

        viewModel.observeViewEvents { onViewEvents(it) }

        viewModel.onEach { renderState(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.removeFragmentOnAttachListener(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        viewModel.handle(SharedSecureStorageAction.Back)
    }

    private fun renderState(state: SharedSecureStorageViewState) {
        if (!state.ready) return
        val fragment =
                when (state.step) {
                    SharedSecureStorageViewState.Step.EnterPassphrase -> SharedSecuredStoragePassphraseFragment::class
                    SharedSecureStorageViewState.Step.EnterKey -> SharedSecuredStorageKeyFragment::class
                    SharedSecureStorageViewState.Step.ResetAll -> SharedSecuredStorageResetAllFragment::class
                }

        showFragment(fragment)
    }

    private fun onViewEvents(it: SharedSecureStorageViewEvent) {
        when (it) {
            is SharedSecureStorageViewEvent.Dismiss -> {
                finish()
            }
            is SharedSecureStorageViewEvent.Error -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(getString(CommonStrings.dialog_title_error))
                        .setMessage(it.message)
                        .setCancelable(false)
                        .setPositiveButton(CommonStrings.ok) { _, _ ->
                            if (it.dismiss) {
                                finish()
                            }
                        }
                        .show()
            }
            is SharedSecureStorageViewEvent.ShowModalLoading -> {
                showWaitingView()
            }
            is SharedSecureStorageViewEvent.HideModalLoading -> {
                hideWaitingView()
            }
            is SharedSecureStorageViewEvent.UpdateLoadingState -> {
                updateWaitingView(it.waitingData)
            }
            is SharedSecureStorageViewEvent.FinishSuccess -> {
                val dataResult = Intent()
                dataResult.putExtra(EXTRA_DATA_RESULT, it.cypherResult)
                setResult(RESULT_OK, dataResult)
                finish()
            }
            is SharedSecureStorageViewEvent.ShowResetBottomSheet -> {
                navigator.open4SSetup(this, SetupMode.HARD_RESET)
            }
            else -> Unit
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is VectorBaseBottomSheetDialogFragment<*>) {
            fragment.resultListener = this
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>) {
        if (supportFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            replaceFragment(
                    views.container,
                    fragmentClass.java,
                    null,
                    fragmentClass.simpleName
            )
        }
    }

    companion object {
        const val EXTRA_DATA_RESULT = "EXTRA_DATA_RESULT"
        const val EXTRA_DATA_RESET = "EXTRA_DATA_RESET"
        const val DEFAULT_RESULT_KEYSTORE_ALIAS = "SharedSecureStorageActivity"

        fun newReadIntent(
                context: Context,
                keyId: String? = null,
                requestedSecrets: List<String>,
                resultKeyStoreAlias: String = DEFAULT_RESULT_KEYSTORE_ALIAS,
                initialStep: SharedSecureStorageViewState.Step = SharedSecureStorageViewState.Step.EnterPassphrase
        ): Intent {
            require(requestedSecrets.isNotEmpty())
            return Intent(context, SharedSecureStorageActivity::class.java).also {
                it.putExtra(
                        Mavericks.KEY_ARG,
                        Args(
                                keyId = keyId,
                                requestedSecrets = requestedSecrets,
                                resultKeyStoreAlias = resultKeyStoreAlias,
                                currentStep = initialStep
                        )
                )
            }
        }

        fun newWriteIntent(
                context: Context,
                keyId: String? = null,
                writeSecrets: List<Pair<String, String>>,
                resultKeyStoreAlias: String = DEFAULT_RESULT_KEYSTORE_ALIAS,
                initialStep: SharedSecureStorageViewState.Step = SharedSecureStorageViewState.Step.EnterPassphrase
        ): Intent {
            require(writeSecrets.isNotEmpty())
            return Intent(context, SharedSecureStorageActivity::class.java).also {
                it.putExtra(
                        Mavericks.KEY_ARG,
                        Args(
                                keyId = keyId,
                                writeSecrets = writeSecrets,
                                resultKeyStoreAlias = resultKeyStoreAlias,
                                currentStep = initialStep,
                        )
                )
            }
        }
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == VectorBaseBottomSheetDialogFragment.ResultListener.RESULT_OK) {
            // the 4S has been reset
            setResult(Activity.RESULT_OK, Intent().apply { putExtra(EXTRA_DATA_RESET, true) })
            finish()
        }
    }
}
