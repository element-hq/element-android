package im.vector.matrix.android.api.session.room

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Cancelable

interface SendService {

    fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable


}