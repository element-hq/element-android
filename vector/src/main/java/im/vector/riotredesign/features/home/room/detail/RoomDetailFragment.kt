/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.detail

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.fragmentViewModel
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.config.Configurations
import com.jaiselrahman.filepicker.model.MediaFile
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.dialogs.DialogListItem
import im.vector.riotredesign.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.utils.*
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.home.HomePermalinkHandler
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.helper.EndlessRecyclerViewScrollListener
import im.vector.riotredesign.features.media.MediaContentRenderer
import im.vector.riotredesign.features.media.MediaViewerActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_detail.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope
import org.koin.core.parameter.parametersOf
import timber.log.Timber


@Parcelize
data class RoomDetailArgs(
        val roomId: String,
        val eventId: String? = null
) : Parcelable


private const val CAMERA_VALUE_TITLE = "attachment"
private const val REQUEST_FILES_REQUEST_CODE = 0
private const val TAKE_IMAGE_REQUEST_CODE = 1

class RoomDetailFragment : RiotFragment(), TimelineEventController.Callback {

    companion object {

        fun newInstance(args: RoomDetailArgs): RoomDetailFragment {
            return RoomDetailFragment().apply {
                setArguments(args)
            }
        }
    }

    private val roomDetailViewModel: RoomDetailViewModel by fragmentViewModel()
    private val timelineEventController: TimelineEventController by inject { parametersOf(this) }
    private val homePermalinkHandler: HomePermalinkHandler by inject()

    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(HomeModule.ROOM_DETAIL_SCOPE))
        setupRecyclerView()
        setupToolbar()
        setupSendButton()
        setupAttachmentButton()
        roomDetailViewModel.subscribe { renderState(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_FILES_REQUEST_CODE, TAKE_IMAGE_REQUEST_CODE -> handleMediaIntent(data)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        roomDetailViewModel.process(RoomDetailActions.IsDisplayed)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun setupToolbar() {
        val parentActivity = riotActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(toolbar)
        }
    }

    private fun setupRecyclerView() {
        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(recyclerView)
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        scrollOnNewMessageCallback = ScrollOnNewMessageCallback(layoutManager)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        timelineEventController.addModelBuildListener {
            it.dispatchTo(stateRestorer)
            it.dispatchTo(scrollOnNewMessageCallback)
        }

        recyclerView.addOnScrollListener(
                EndlessRecyclerViewScrollListener(layoutManager, RoomDetailViewModel.PAGINATION_COUNT) { direction ->
                    roomDetailViewModel.process(RoomDetailActions.LoadMore(direction))
                })
        recyclerView.setController(timelineEventController)
        timelineEventController.callback = this
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val textMessage = composerEditText.text.toString()
            if (textMessage.isNotBlank()) {
                roomDetailViewModel.process(RoomDetailActions.SendMessage(textMessage))
                composerEditText.text = null
            }
        }
    }

    private fun setupAttachmentButton() {
        attachmentButton.setOnClickListener {
            val intent = Intent(requireContext(), FilePickerActivity::class.java)
            intent.putExtra(FilePickerActivity.CONFIGS, Configurations.Builder()
                    .setCheckPermission(true)
                    .setShowFiles(true)
                    .setShowAudios(true)
                    .setSkipZeroSizeFiles(true)
                    .build())
            startActivityForResult(intent, REQUEST_FILES_REQUEST_CODE)
            /*
            val items = ArrayList<DialogListItem>()
            // Send file
            items.add(DialogListItem.SendFile)
            // Send voice

            if (PreferencesManager.isSendVoiceFeatureEnabled(this)) {
                items.add(DialogListItem.SendVoice.INSTANCE)
            }


            // Send sticker
            //items.add(DialogListItem.SendSticker)
            // Camera

            //if (PreferencesManager.useNativeCamera(this)) {
            items.add(DialogListItem.TakePhoto)
            items.add(DialogListItem.TakeVideo)
            //} else {
//                items.add(DialogListItem.TakePhotoVideo.INSTANCE)
            //          }
            val adapter = DialogSendItemAdapter(requireContext(), items)
            AlertDialog.Builder(requireContext())
                    .setAdapter(adapter) { _, position ->
                        onSendChoiceClicked(items[position])
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                    */
        }
    }

    private fun onSendChoiceClicked(dialogListItem: DialogListItem) {
        Timber.v("On send choice clicked: $dialogListItem")
        when (dialogListItem) {
            is DialogListItem.SendFile       -> {
                // launchFileIntent
            }
            is DialogListItem.SendVoice      -> {
                //launchAudioRecorderIntent()
            }
            is DialogListItem.SendSticker    -> {
                //startStickerPickerActivity()
            }
            is DialogListItem.TakePhotoVideo -> if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
                //    launchCamera()
            }
            is DialogListItem.TakePhoto      -> if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_CAMERA)) {
                openCamera(requireActivity(), CAMERA_VALUE_TITLE, TAKE_IMAGE_REQUEST_CODE)
            }
            is DialogListItem.TakeVideo      -> if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_VIDEO_CAMERA)) {
                //  launchNativeVideoRecorder()
            }
        }
    }

    private fun handleMediaIntent(data: Intent) {
        val files: ArrayList<MediaFile> = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES)
        roomDetailViewModel.process(RoomDetailActions.SendMedia(files))
    }

    private fun renderState(state: RoomDetailViewState) {
        renderRoomSummary(state)
        timelineEventController.setTimeline(state.timeline)
    }

    private fun renderRoomSummary(state: RoomDetailViewState) {
        state.asyncRoomSummary()?.let {
            toolbarTitleView.text = it.displayName
            AvatarRenderer.render(it, toolbarAvatarImageView)
            if (it.topic.isNotEmpty()) {
                toolbarSubtitleView.visibility = View.VISIBLE
                toolbarSubtitleView.text = it.topic
            } else {
                toolbarSubtitleView.visibility = View.GONE
            }
        }
    }

// TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String) {
        homePermalinkHandler.launch(url)
    }

    override fun onEventVisible(event: TimelineEvent) {
        roomDetailViewModel.process(RoomDetailActions.EventDisplayed(event))
    }

    override fun onMediaClicked(mediaData: MediaContentRenderer.Data, view: View) {
        val intent = MediaViewerActivity.newIntent(riotActivity, mediaData)
        startActivity(intent)
    }

}
