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
package im.vector.app.features.widgets.permissions

import android.content.DialogInterface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetRoomWidgetPermissionBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.widgets.WidgetArgs
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class RoomWidgetPermissionBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetRoomWidgetPermissionBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetRoomWidgetPermissionBinding {
        return BottomSheetRoomWidgetPermissionBinding.inflate(inflater, container, false)
    }

    private val viewModel: RoomWidgetPermissionViewModel by activityViewModel()

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override val showExpanded = true

    // Use this if you don't need the full activity view model
    var directListener: ((Boolean) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.widgetPermissionDecline.debouncedClicks { doDecline() }
        views.widgetPermissionContinue.debouncedClicks { doAccept() }
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val permissionData = state.permissionData() ?: return@withState
        views.widgetPermissionOwnerId.text = permissionData.widget.senderInfo?.userId ?: ""
        views.widgetPermissionOwnerDisplayName.text = permissionData.widget.senderInfo?.disambiguatedDisplayName
        permissionData.widget.senderInfo?.toMatrixItem()?.also {
            avatarRenderer.render(it, views.widgetPermissionOwnerAvatar)
        }

        val domain = permissionData.widgetDomain ?: ""
        val infoBuilder = SpannableStringBuilder()
                .append(getString(
                        R.string.room_widget_permission_webview_shared_info_title
                                .takeIf { permissionData.isWebviewWidget }
                                ?: R.string.room_widget_permission_shared_info_title,
                        "'$domain'"))
        infoBuilder.append("\n")
        permissionData.permissionsList.forEach {
            infoBuilder.append("\n")
            val bulletPoint = getString(it)
            infoBuilder.append(bulletPoint, BulletSpan(resources.getDimension(R.dimen.quote_gap).toInt()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        infoBuilder.append("\n")
        views.widgetPermissionSharedInfo.text = infoBuilder
    }

    private fun doDecline() {
        viewModel.handle(RoomWidgetPermissionActions.BlockWidget)
        directListener?.invoke(false)
        // optimistic dismiss
        dismiss()
    }

    private fun doAccept() {
        viewModel.handle(RoomWidgetPermissionActions.AllowWidget)
        directListener?.invoke(true)
        // optimistic dismiss
        dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.handle(RoomWidgetPermissionActions.DoClose)
    }

    companion object {

        fun newInstance(widgetArgs: WidgetArgs): RoomWidgetPermissionBottomSheet {
            return RoomWidgetPermissionBottomSheet().apply {
                setArguments(widgetArgs)
            }
        }
    }
}
