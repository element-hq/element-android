package im.vector.riotx.features.share

import android.content.Intent
import android.os.Bundle
import com.kbeanie.multipicker.api.AudioPicker
import com.kbeanie.multipicker.api.FilePicker
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.VideoPicker
import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.callbacks.VideoPickerCallback
import com.kbeanie.multipicker.api.entity.ChosenAudio
import com.kbeanie.multipicker.api.entity.ChosenFile
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.kbeanie.multipicker.api.entity.ChosenVideo
import com.kbeanie.multipicker.utils.IntentUtils
import im.vector.riotx.core.platform.VectorBaseActivity
import timber.log.Timber


class IncomingShareActivity :
        VectorBaseActivity(),
        ImagePickerCallback,
        VideoPickerCallback,
        FilePickerCallback,
        AudioPickerCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            val handled = handleShare(intent)
            if (!handled) {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun handleShare(intent: Intent): Boolean {
        val type = intent.type ?: return false
        if (type.startsWith("image")) {
            val picker = ImagePicker(this)
            picker.setImagePickerCallback(this)
            picker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("video")) {
            val picker = VideoPicker(this)
            picker.setVideoPickerCallback(this)
            picker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("application") || type.startsWith("file") || type.startsWith("*")) {
            val picker = FilePicker(this)
            picker.setFilePickerCallback(this)
            picker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else if (type.startsWith("audio")) {
            val picker = AudioPicker(this)
            picker.setAudioPickerCallback(this)
            picker.submit(IntentUtils.getPickerIntentForSharing(intent))
        } else {
            return false
        }
        return true
    }

    override fun onAudiosChosen(p0: MutableList<ChosenAudio>?) {
        Timber.v("On audios chosen $p0")
    }

    override fun onFilesChosen(p0: MutableList<ChosenFile>?) {
        Timber.v("On files chosen $p0")
    }

    override fun onImagesChosen(p0: MutableList<ChosenImage>?) {
        Timber.v("On images chosen $p0")
    }

    override fun onError(p0: String?) {
        Timber.v("On error")
    }

    override fun onVideosChosen(p0: MutableList<ChosenVideo>?) {
        Timber.v("On videos chosen $p0")
    }

}