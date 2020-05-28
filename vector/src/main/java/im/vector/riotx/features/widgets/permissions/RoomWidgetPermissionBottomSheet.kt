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
package im.vector.riotx.features.widgets.permissions

import android.os.Build
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.withArgs
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.widgets.WidgetArgs
import kotlinx.android.parcel.Parcelize
import javax.inject.Inject

class RoomWidgetPermissionBottomSheet : VectorBaseBottomSheetDialogFragment() {

    override fun getLayoutResId(): Int = R.layout.bottom_sheet_room_widget_permission

    private val viewModel: RoomWidgetPermissionViewModel by fragmentViewModel()

    @BindView(R.id.bottom_sheet_widget_permission_shared_info)
    lateinit var sharedInfoTextView: TextView

    @BindView(R.id.bottom_sheet_widget_permission_owner_id)
    lateinit var authorIdText: TextView

    @BindView(R.id.bottom_sheet_widget_permission_owner_display_name)
    lateinit var authorNameText: TextView

    @BindView(R.id.bottom_sheet_widget_permission_owner_avatar)
    lateinit var authorAvatarView: ImageView

    @Inject lateinit var avatarRenderer: AvatarRenderer

    var onFinish: ((Boolean) -> Unit)? = null

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val permissionData = state.permissionData() ?: return@withState
        authorIdText.text = permissionData.widget.senderInfo?.userId ?: ""
        authorNameText.text = permissionData.widget.senderInfo?.disambiguatedDisplayName
        permissionData.widget.senderInfo?.toMatrixItem()?.also {
            avatarRenderer.render(it, authorAvatarView)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                infoBuilder.append(bulletPoint, BulletSpan(resources.getDimension(R.dimen.quote_gap).toInt()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val start = infoBuilder.length
                infoBuilder.append(bulletPoint)
                infoBuilder.setSpan(
                        BulletSpan(resources.getDimension(R.dimen.quote_gap).toInt()),
                        start,
                        bulletPoint.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        infoBuilder.append("\n")

        sharedInfoTextView.text = infoBuilder
    }

    @OnClick(R.id.bottom_sheet_widget_permission_decline_button)
    fun doDecline() {
        viewModel.handle(RoomWidgetPermissionActions.BlockWidget)
        //optimistic dismiss
        dismiss()
        onFinish?.invoke(false)
    }

    @OnClick(R.id.bottom_sheet_widget_permission_continue_button)
    fun doAccept() {
        viewModel.handle(RoomWidgetPermissionActions.AllowWidget)
        //optimistic dismiss
        dismiss()
        onFinish?.invoke(true)
    }

    companion object {

        fun newInstance(widgetArgs: WidgetArgs) = RoomWidgetPermissionBottomSheet().withArgs {
            putParcelable(MvRx.KEY_ARG, widgetArgs)
        }
    }
}
