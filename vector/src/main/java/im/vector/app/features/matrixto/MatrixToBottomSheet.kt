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

package im.vector.app.features.matrixto

import android.os.Bundle
import android.view.View
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.home.AvatarRenderer
import kotlinx.android.synthetic.main.bottom_sheet_matrix_to_card.*
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

class MatrixToBottomSheet(private val matrixItem: MatrixItem) : VectorBaseBottomSheetDialogFragment() {

    @Inject lateinit var avatarRenderer: AvatarRenderer

    interface InteractionListener {
        fun didTapStartMessage(matrixItem: MatrixItem)
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    private var interactionListener: InteractionListener? = null

    override fun getLayoutResId() = R.layout.bottom_sheet_matrix_to_card

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        matrixToCardSendMessageButton.debouncedClicks {
            interactionListener?.didTapStartMessage(matrixItem)
            dismiss()
        }

        matrixToCardNameText.setTextOrHide(matrixItem.displayName)
        matrixToCardUserIdText.setTextOrHide(matrixItem.id)
        avatarRenderer.render(matrixItem, matrixToCardAvatar)
    }

    companion object {
        fun create(matrixItem: MatrixItem, listener: InteractionListener?): MatrixToBottomSheet {
            return MatrixToBottomSheet(matrixItem).apply {
                interactionListener = listener
            }
        }
    }
}
