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

package im.vector.riotx.attachment_viewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


abstract class BaseViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

    abstract fun bind(attachmentInfo: AttachmentInfo)
}


class AttachmentViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    override fun bind(attachmentInfo: AttachmentInfo) {

    }
}

//class AttachmentsAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
class AttachmentsAdapter() : RecyclerView.Adapter<BaseViewHolder>() {

    var attachmentSourceProvider: AttachmentSourceProvider? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_image_attachment -> ZoomableImageViewHolder(itemView)
            R.layout.item_animated_image_attachment -> AnimatedImageViewHolder(itemView)
            else                           -> AttachmentViewHolder(itemView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val info = attachmentSourceProvider!!.getAttachmentInfoAt(position)
        return when (info) {
            is AttachmentInfo.Image -> R.layout.item_image_attachment
            is AttachmentInfo.Video -> R.layout.item_video_attachment
            is AttachmentInfo.AnimatedImage -> R.layout.item_animated_image_attachment
            is AttachmentInfo.Audio -> TODO()
            is AttachmentInfo.File  -> TODO()
        }

    }

    override fun getItemCount(): Int {
        return attachmentSourceProvider?.getItemCount() ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        attachmentSourceProvider?.getAttachmentInfoAt(position)?.let {
            holder.bind(it)
            when(it) {
                is AttachmentInfo.Image -> {
                    attachmentSourceProvider?.loadImage(holder as ZoomableImageViewHolder, it)
                }
                is AttachmentInfo.AnimatedImage -> {
                    attachmentSourceProvider?.loadImage(holder as AnimatedImageViewHolder, it)
                }
                else                    -> {}
            }

        }
    }

    fun isScaled(position: Int): Boolean {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position)
        if (holder is ZoomableImageViewHolder) {
            return holder.touchImageView.attacher.scale > 1f
        }
        return false
    }

//    override fun getItemCount(): Int {
//        return 8
//    }
//
//    override fun createFragment(position: Int): Fragment {
//        // Return a NEW fragment instance in createFragment(int)
//        val fragment = DemoObjectFragment()
//        fragment.arguments = Bundle().apply {
//            // Our object is just an integer :-P
//            putInt(ARG_OBJECT, position + 1)
//        }
//        return fragment
//    }

}


//private const val ARG_OBJECT = "object"
//
//// Instances of this class are fragments representing a single
//// object in our collection.
//class DemoObjectFragment : Fragment() {
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.view_image_attachment, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
//            val textView: TextView = view.findViewById(R.id.testPage)
//            textView.text = getInt(ARG_OBJECT).toString()
//        }
//    }
//}
