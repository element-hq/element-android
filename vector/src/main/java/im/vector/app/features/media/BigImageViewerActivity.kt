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

package im.vector.app.features.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_LAUNCH_CAMERA
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.entity.MultiPickerImageType
import kotlinx.android.synthetic.main.activity_big_image_viewer.*
import java.io.File
import javax.inject.Inject

class BigImageViewerActivity : VectorBaseActivity() {
    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var stringProvider: StringProvider

    private var uri: Uri? = null

    override fun getMenuRes() = R.menu.vector_big_avatar_viewer

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_big_image_viewer)

        setSupportActionBar(bigImageViewerToolbar)
        supportActionBar?.apply {
            title = intent.getStringExtra(EXTRA_TITLE)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        uri = sessionHolder.getSafeActiveSession()
                ?.contentUrlResolver()
                ?.resolveFullSize(intent.getStringExtra(EXTRA_IMAGE_URL))
                ?.toUri()

        if (uri == null) {
            finish()
        } else {
            bigImageViewerImageView.showImage(uri)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.bigAvatarEditAction).isVisible = shouldShowEditAction()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.bigAvatarEditAction) {
            showAvatarSelector()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shouldShowEditAction(): Boolean {
        return uri != null && intent.getBooleanExtra(EXTRA_CAN_EDIT_IMAGE, false)
    }

    private fun showAvatarSelector() {
        AlertDialog.Builder(this)
                .setItems(arrayOf(
                        stringProvider.getString(R.string.attachment_type_camera),
                        stringProvider.getString(R.string.attachment_type_gallery)
                )) { dialog, which ->
                    dialog.cancel()
                    onAvatarTypeSelected(isCamera = (which == 0))
                }
                .show()
    }

    private var avatarCameraUri: Uri? = null
    private fun onAvatarTypeSelected(isCamera: Boolean) {
        if (isCamera) {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
                avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(this)
            }
        } else {
            MultiPicker.get(MultiPicker.IMAGE).single().startWith(this)
        }
    }

    private fun onRoomAvatarSelected(image: MultiPickerImageType) {
        val destinationFile = File(cacheDir, "${image.displayName}_edited_image_${System.currentTimeMillis()}")
        val uri = image.contentUri
        createUCropWithDefaultSettings(this, uri, destinationFile.toUri(), image.displayName)
                .apply { withAspectRatio(1f, 1f) }
                .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                MultiPicker.REQUEST_CODE_TAKE_PHOTO -> {
                    avatarCameraUri?.let { uri ->
                        MultiPicker.get(MultiPicker.CAMERA)
                                .getTakenPhoto(this, requestCode, resultCode, uri)
                                ?.let {
                                    onRoomAvatarSelected(it)
                                }
                    }
                }
                MultiPicker.REQUEST_CODE_PICK_IMAGE -> {
                    MultiPicker
                            .get(MultiPicker.IMAGE)
                            .getSelectedFiles(this, requestCode, resultCode, data)
                            .firstOrNull()?.let {
                                // TODO. UCrop library cannot read from Gallery. For now, we will set avatar as it is.
                                // onRoomAvatarSelected(it)
                                onAvatarCropped(it.contentUri)
                            }
                }
                UCrop.REQUEST_CROP                  -> data?.let { onAvatarCropped(UCrop.getOutput(it)) }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            when (requestCode) {
                PERMISSION_REQUEST_CODE_LAUNCH_CAMERA -> onAvatarTypeSelected(true)
            }
        }
    }

    private fun onAvatarCropped(uri: Uri?) {
        if (uri != null) {
            setResult(Activity.RESULT_OK, Intent().setData(uri))
            this@BigImageViewerActivity.finish()
        } else {
            Toast.makeText(this, "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"
        private const val EXTRA_CAN_EDIT_IMAGE = "EXTRA_CAN_EDIT_IMAGE"
        const val REQUEST_CODE = 1000

        fun newIntent(context: Context, title: String?, imageUrl: String, canEditImage: Boolean = false): Intent {
            return Intent(context, BigImageViewerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_CAN_EDIT_IMAGE, canEditImage)
            }
        }
    }
}
