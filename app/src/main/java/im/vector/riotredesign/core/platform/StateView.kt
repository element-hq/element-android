package im.vector.riotredesign.core.platform

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import im.vector.riotredesign.R
import kotlinx.android.synthetic.main.view_state.view.*

class StateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    sealed class State {
        object Content : State()
        object Loading : State()
        data class Empty(val message: CharSequence? = null) : State()
        data class Error(val message: CharSequence? = null) : State()
    }


    private var eventCallback: EventCallback? = null

    var contentView: View? = null

    var state: State = State.Empty()
        set(newState) {
            if (newState != state) {
                update(newState)
            }
        }

    interface EventCallback {
        fun onRetryClicked()
    }

    init {
        View.inflate(context, R.layout.view_state, this)
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        errorRetryView.setOnClickListener {
            eventCallback?.onRetryClicked()
        }
        state = State.Content
    }


    private fun update(newState: State) {
        when (newState) {
            is StateView.State.Content -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.INVISIBLE
                contentView?.visibility = View.VISIBLE
            }
            is StateView.State.Loading -> {
                progressBar.visibility = View.VISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.INVISIBLE
                contentView?.visibility = View.INVISIBLE
            }
            is StateView.State.Empty -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.INVISIBLE
                emptyView.visibility = View.VISIBLE
                emptyMessageView.text = newState.message
                if (contentView != null) {
                    contentView!!.visibility = View.INVISIBLE
                }
            }
            is StateView.State.Error -> {
                progressBar.visibility = View.INVISIBLE
                errorView.visibility = View.VISIBLE
                emptyView.visibility = View.INVISIBLE
                errorMessageView.text = newState.message
                if (contentView != null) {
                    contentView!!.visibility = View.INVISIBLE
                }
            }
        }
    }
}
