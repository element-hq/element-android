package im.vector.riotredesign.features.home.room.detail.timeline.action

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.riotredesign.core.utils.LiveEvent

/**
 * Activity shared view model to handle message actions
 */
class ActionsHandler : ViewModel() {

    data class ActionData(
            val actionId: String,
            val data: Any?
    )

    val actionCommandEvent = MutableLiveData<LiveEvent<ActionData>>()

    fun fireAction(actionId: String, data: Any? = null) {
        actionCommandEvent.value = LiveEvent(ActionData(actionId,data))
    }

}