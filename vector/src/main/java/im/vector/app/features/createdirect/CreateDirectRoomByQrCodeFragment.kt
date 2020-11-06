/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.createdirect

import android.widget.Toast
import com.airbnb.mvrx.activityViewModel
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.userdirectory.PendingInvitee
import kotlinx.android.synthetic.main.fragment_qr_code_scanner.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class CreateDirectRoomByQrCodeFragment @Inject constructor() : VectorBaseFragment(), ZXingScannerView.ResultHandler {

    private val viewModel: CreateDirectRoomViewModel by activityViewModel()

    override fun getLayoutResId() = R.layout.fragment_qr_code_scanner

    override fun onResume() {
        super.onResume()
        // Register ourselves as a handler for scan results.
        scannerView.setResultHandler(null)
        // Start camera on resume
        scannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        // Stop camera on pause
        scannerView.stopCamera()
    }

    // Copied from https://github.com/markusfisch/BinaryEye/blob/
    // 9d57889b810dcaa1a91d7278fc45c262afba1284/app/src/main/kotlin/de/markusfisch/android/binaryeye/activity/CameraActivity.kt#L434
    private fun getRawBytes(result: Result): ByteArray? {
        val metadata = result.resultMetadata ?: return null
        val segments = metadata[ResultMetadataType.BYTE_SEGMENTS] ?: return null
        var bytes = ByteArray(0)
        @Suppress("UNCHECKED_CAST")
        for (seg in segments as Iterable<ByteArray>) {
            bytes += seg
        }
        // byte segments can never be shorter than the text.
        // Zxing cuts off content prefixes like "WIFI:"
        return if (bytes.size >= result.text.length) bytes else null
    }

    private fun addByQrCode(value: String) {
        val mxid = (PermalinkParser.parse(value) as? PermalinkData.UserLink)?.userId

        if (mxid === null) {
            Toast.makeText(requireContext(), R.string.invalid_qr_code_uri, Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        } else {
            // This and the related conditional can be removed when PR #2342 is merged
            val existingDm = viewModel.session.getExistingDirectRoomWithUser(mxid)

            if (existingDm === null) {
                // The following assumes MXIDs are case insensitive
                if (mxid.equals(other = viewModel.session.myUserId, ignoreCase = true)) {
                    Toast.makeText(requireContext(), R.string.cannot_dm_self, Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                } else {
                    // Try to get user from known users and fall back to creating a User object from MXID
                    val qrInvitee = if (viewModel.session.getUser(mxid) != null) viewModel.session.getUser(mxid)!! else User(mxid, null, null)

                    viewModel.handle(
                            CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers(setOf(PendingInvitee.UserPendingInvitee(qrInvitee)))
                    )
                }
            } else {
                navigator.openRoom(requireContext(), existingDm, null, false)
                requireActivity().finish()
            }
        }
    }

    override fun handleResult(result: Result?) {
        if (result === null) {
            Toast.makeText(requireContext(), R.string.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        } else {
            val rawBytes = getRawBytes(result)
            val rawBytesStr = rawBytes?.toString(Charsets.ISO_8859_1)
            val value = rawBytesStr ?: result.text
            addByQrCode(value)
        }
    }
}
