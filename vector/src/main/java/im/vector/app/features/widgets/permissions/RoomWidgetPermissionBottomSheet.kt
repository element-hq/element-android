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
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import butterknife.OnClick
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import org.matrix.android.sdk.api.util.toMatrixItem
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.withArgs
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.widgets.WidgetArgs
import kotlinx.android.synthetic.main.bottom_sheet_room_widget_permission.*
import javax.inject.Inject

class RoomWidgetPermissionBottomSheet : VectorBaseBottomSheetDialogFragment() {

    override fun getLayoutResId(): Int = R.layout.bottom_sheet_room_widget_permission

    private val viewModel: RoomWidgetPermissionViewModel by activityViewModel()

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override val showExpanded = true

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    // Use this if you don't need the full activity view model
    var directListener: ((Boolean) -> Unit)? = null

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val permissionData = state.permissionData() ?: return@withState
        widgetPermissionOwnerId.text = permissionData.widget.senderInfo?.userId ?: ""
        widgetPermissionOwnerDisplayName.text = permissionData.widget.senderInfo?.disambiguatedDisplayName
        permissionData.widget.senderInfo?.toMatrixItem()?.also {
            avatarRenderer.render(it, widgetPermissionOwnerAvatar)
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
        widgetPermissionSharedInfo.text = infoBuilder
    }

    @OnClick(R.id.widgetPermissionDecline)
    fun doDecline() {
        viewModel.handle(RoomWidgetPermissionActions.BlockWidget)
        directListener?.invoke(false)
        // optimistic dismiss
        dismiss()
    }

    @OnClick(R.id.widgetPermissionContinue)
    fun doAccept() {
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

        fun newInstance(widgetArgs: WidgetArgs) = RoomWidgetPermissionBottomSheet().withArgs {
            putParcelable(MvRx.KEY_ARG, widgetArgs)
        }
    }
}
