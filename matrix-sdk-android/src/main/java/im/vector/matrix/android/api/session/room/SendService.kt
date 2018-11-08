package im.vector.matrix.android.api.session.room

import im.vector.matrix.android.api.util.Cancelable

interface SendService {

    fun sendTextMessage(text: String): Cancelable


}