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

package im.vector.riotx.features.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.net.toUri
import com.yalantis.ucrop.UCrop
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.features.roomprofile.AvatarSelectorView
import im.vector.riotx.multipicker.MultiPicker
import im.vector.riotx.multipicker.entity.MultiPickerImageType
import kotlinx.android.synthetic.main.activity_big_image_viewer.*
import java.io.File
import javax.inject.Inject

class BigImageViewerActivity : VectorBaseActivity(), AvatarSelectorView.Callback {
    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var colorProvider: ColorProvider

    private var uri: Uri? = null
    private lateinit var avatarSelector: AvatarSelectorView

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
        if (!::avatarSelector.isInitialized) {
            avatarSelector = AvatarSelectorView(this, layoutInflater, this)
        }
        avatarSelector.show(bigImageViewerToolbar, false)
    }

    private var avatarCameraUri: Uri? = null
    override fun onTypeSelected(type: AvatarSelectorView.Type) {
        when (type) {
            AvatarSelectorView.Type.CAMERA  -> {
                avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(this)
            }
            AvatarSelectorView.Type.GALLERY -> {
                MultiPicker.get(MultiPicker.IMAGE).single().startWith(this)
            }
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
