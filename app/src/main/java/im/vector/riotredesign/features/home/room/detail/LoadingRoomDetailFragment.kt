package im.vector.riotredesign.features.home.room.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import kotlinx.android.synthetic.main.fragment_loading_room_detail.*

class LoadingRoomDetailFragment : RiotFragment() {

    companion object {

        fun newInstance(): LoadingRoomDetailFragment {
            return LoadingRoomDetailFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loading_room_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this)
                .load(R.drawable.riot_splash)
                .into(animatedLogoImageView)
    }


}