/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.createJSonViewerStyleProvider
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.lib.strings.CommonStrings
import org.billcarsonfr.jsonviewer.JSonViewerDialog
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.util.MatrixJsonParser
import javax.inject.Inject

@AndroidEntryPoint
class AccountDataFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        AccountDataEpoxyController.InteractionListener {

    @Inject lateinit var epoxyController: AccountDataEpoxyController
    @Inject lateinit var colorProvider: ColorProvider

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: AccountDataViewModel by fragmentViewModel(AccountDataViewModel::class)

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.settings_account_data)
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(epoxyController, dividerDrawable = R.drawable.divider_horizontal)
        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        epoxyController.interactionListener = null
        super.onDestroyView()
    }

    override fun didTap(data: UserAccountDataEvent) {
        val jsonString = MatrixJsonParser.getMoshi()
                .adapter(UserAccountDataEvent::class.java)
                .toJson(data)
        JSonViewerDialog.newInstance(
                jsonString,
                -1, // open All
                createJSonViewerStyleProvider(colorProvider)
        ).show(childFragmentManager, "JSON_VIEWER")
    }

    override fun didLongTap(data: UserAccountDataEvent) {
        MaterialAlertDialogBuilder(requireActivity(), im.vector.lib.ui.styles.R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(CommonStrings.action_delete)
                .setMessage(getString(CommonStrings.delete_account_data_warning, data.type))
                .setNegativeButton(CommonStrings.action_cancel, null)
                .setPositiveButton(CommonStrings.action_delete) { _, _ ->
                    viewModel.handle(AccountDataAction.DeleteAccountData(data.type))
                }
                .show()
    }
}
