package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.themes.ThemeUtils

class MessageMenuFragment : BaseMvRxFragment() {

    private val viewModel: MessageMenuViewModel by fragmentViewModel(MessageMenuViewModel::class)

    private var addSeparators = false

    var interactionListener: InteractionListener? = null

    override fun invalidate() = withState(viewModel) { state ->

        val linearLayout = view as? LinearLayout
        if (linearLayout != null) {
            val inflater = LayoutInflater.from(linearLayout.context)
            linearLayout.removeAllViews()
            var insertIndex = 0
            state.actions.forEachIndexed { index, action ->
                inflateActionView(action, inflater, linearLayout)?.let {
                    it.setOnClickListener {
                        interactionListener?.didSelectMenuAction(action)
                    }
                    linearLayout.addView(it, insertIndex)
                    insertIndex++
                    if (addSeparators) {
                        if (index < state.actions.size - 1) {
                            linearLayout.addView(inflateSeparatorView(), insertIndex)
                            insertIndex++
                        }
                    }
                }
            }
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //we just create programmatically
        val contentView = LinearLayout(context)
        contentView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        contentView.orientation = LinearLayout.VERTICAL
        return contentView
    }

    private fun inflateActionView(action: SimpleAction, inflater: LayoutInflater, container: ViewGroup?): View? {
        return inflater.inflate(R.layout.adapter_item_action, container, false)?.apply {
            if (action.iconResId != null) {
                findViewById<ImageView>(R.id.action_icon)?.setImageResource(action.iconResId)
            } else {
                findViewById<ImageView>(R.id.action_icon)?.setImageDrawable(null)
            }
            findViewById<TextView>(R.id.action_title)?.setText(action.titleRes)
        }
    }

    private fun inflateSeparatorView(): View {
        val frame = FrameLayout(context)
        frame.setBackgroundColor(ThemeUtils.getColor(requireContext(), R.attr.vctr_list_divider_color))
        frame.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, requireContext().resources.displayMetrics.density.toInt())
        return frame

    }

    interface InteractionListener {
        fun didSelectMenuAction(simpleAction: SimpleAction)
    }


    companion object {
        fun newInstance(pa: MessageActionsBottomSheet.ParcelableArgs): MessageMenuFragment {
            val args = Bundle()
            args.putParcelable(MvRx.KEY_ARG, pa)
            val fragment = MessageMenuFragment()
            fragment.arguments = args
            return fragment
        }
    }
}