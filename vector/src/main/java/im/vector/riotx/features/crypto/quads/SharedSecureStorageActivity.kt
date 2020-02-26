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

package im.vector.riotx.features.crypto.quads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.platform.SimpleFragmentActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity.*
import javax.inject.Inject

class SharedSecureStorageActivity : SimpleFragmentActivity() {

    @Parcelize
    data class Args(
            val keyId: String?,
            val requestedSecrets: List<String>,
            val resultKeyStoreAlias: String
    ) : Parcelable

    private val viewModel: SharedSecureStorageViewModel by viewModel()
    @Inject lateinit var viewModelFactory: SharedSecureStorageViewModel.Factory
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar.visibility = View.GONE
        if (isFirstCreation()) {
            addFragment(R.id.container, SharedSecuredStoragePassphraseFragment::class.java)
        }

        viewModel.viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    observeViewEvents(it)
                }
                .disposeOnDestroy()

        viewModel.subscribe(this) {
            //            renderState(it)
        }
    }

    private fun observeViewEvents(it: SharedSecureStorageViewEvent?) {
        when (it) {
            is SharedSecureStorageViewEvent.Dismiss            -> {
                finish()
            }
            is SharedSecureStorageViewEvent.Error              -> {
                AlertDialog.Builder(this)
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
            is SharedSecureStorageViewEvent.ShowModalLoading   -> {
                showWaitingView()
            }
            is SharedSecureStorageViewEvent.HideModalLoading   -> {
                hideWaitingView()
            }
            is SharedSecureStorageViewEvent.UpdateLoadingState -> {
                updateWaitingView(it.waitingData)
            }
            is SharedSecureStorageViewEvent.FinishSuccess      -> {
                val dataResult = Intent()
                dataResult.putExtra(EXTRA_DATA_RESULT, it.cypherResult)
                setResult(Activity.RESULT_OK, dataResult)
                finish()
            }
        }
    }

//    fun renderState(state: SharedSecureStorageViewState) {
//    }

    companion object {

        const val EXTRA_DATA_RESULT = "EXTRA_DATA_RESULT"
        const val RESULT_KEYSTORE_ALIAS = "SharedSecureStorageActivity"
        fun newIntent(context: Context, keyId: String? = null, requestedSecrets: List<String>, resultKeyStoreAlias: String = RESULT_KEYSTORE_ALIAS): Intent {
            require(requestedSecrets.isNotEmpty())
            return Intent(context, SharedSecureStorageActivity::class.java).also {
                it.putExtra(MvRx.KEY_ARG, Args(
                        keyId,
                        requestedSecrets,
                        resultKeyStoreAlias
                ))
            }
        }
    }
}
