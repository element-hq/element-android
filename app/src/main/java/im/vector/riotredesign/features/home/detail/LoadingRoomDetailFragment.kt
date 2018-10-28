package im.vector.riotredesign.features.home.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment

class LoadingRoomDetailFragment : RiotFragment() {

    companion object {

        fun newInstance(): LoadingRoomDetailFragment {
            return LoadingRoomDetailFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_loading_room_detail, container, false)
    }


}