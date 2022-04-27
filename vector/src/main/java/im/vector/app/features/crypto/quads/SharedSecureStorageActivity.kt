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

package im.vector.app.features.crypto.quads

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
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.crypto.recover.SetupMode
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class SharedSecureStorageActivity :
        SimpleFragmentActivity(),
        VectorBaseBottomSheetDialogFragment.ResultListener,
        FragmentOnAttachListener {

    @Parcelize
    data class Args(
            val keyId: String?,
            val requestedSecrets: List<String>,
            val resultKeyStoreAlias: String
    ) : Parcelable

    private val viewModel: SharedSecureStorageViewModel by viewModel()
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.addFragmentOnAttachListener(this)

        views.toolbar.visibility = View.GONE

        viewModel.observeViewEvents { observeViewEvents(it) }

        viewModel.onEach { renderState(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.removeFragmentOnAttachListener(this)
    }

    override fun onBackPressed() {
        viewModel.handle(SharedSecureStorageAction.Back)
    }

    private fun renderState(state: SharedSecureStorageViewState) {
        if (!state.ready) return
        val fragment =
                when (state.step) {
                    SharedSecureStorageViewState.Step.EnterPassphrase -> SharedSecuredStoragePassphraseFragment::class
                    SharedSecureStorageViewState.Step.EnterKey        -> SharedSecuredStorageKeyFragment::class
                    SharedSecureStorageViewState.Step.ResetAll        -> SharedSecuredStorageResetAllFragment::class
                }

        showFragment(fragment)
    }

    private fun observeViewEvents(it: SharedSecureStorageViewEvent?) {
        when (it) {
            is SharedSecureStorageViewEvent.Dismiss              -> {
                finish()
            }
            is SharedSecureStorageViewEvent.Error                -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(it.message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            if (it.dismiss) {
                                finish()
                            }
                        }
                        .show()
            }
            is SharedSecureStorageViewEvent.ShowModalLoading     -> {
                showWaitingView()
            }
            is SharedSecureStorageViewEvent.HideModalLoading     -> {
                hideWaitingView()
            }
            is SharedSecureStorageViewEvent.UpdateLoadingState   -> {
                updateWaitingView(it.waitingData)
            }
            is SharedSecureStorageViewEvent.FinishSuccess        -> {
                val dataResult = Intent()
                dataResult.putExtra(EXTRA_DATA_RESULT, it.cypherResult)
                setResult(RESULT_OK, dataResult)
                finish()
            }
            is SharedSecureStorageViewEvent.ShowResetBottomSheet -> {
                navigator.open4SSetup(this, SetupMode.HARD_RESET)
            }
            else                                                 -> Unit
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

        fun newIntent(context: Context,
                      keyId: String? = null,
                      requestedSecrets: List<String>,
                      resultKeyStoreAlias: String = DEFAULT_RESULT_KEYSTORE_ALIAS): Intent {
            require(requestedSecrets.isNotEmpty())
            return Intent(context, SharedSecureStorageActivity::class.java).also {
                it.putExtra(Mavericks.KEY_ARG, Args(
                        keyId,
                        requestedSecrets,
                        resultKeyStoreAlias
                ))
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
