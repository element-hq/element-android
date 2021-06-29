package im.vector.app.features.home.room.detail.timeline.helper

import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import javax.inject.Inject
import javax.inject.Singleton

/*
    You can use this to share user power level helpers within the app.
    You should probably use this only in the context of the timeline.
 */
@Singleton
class PowerLevelsHolder @Inject constructor() {

    private var roomHelpers = HashMap<String, PowerLevelsHelper>()

    fun set(roomId: String, powerLevelsHelper: PowerLevelsHelper) {
        roomHelpers[roomId] = powerLevelsHelper
    }

    fun get(roomId: String) = roomHelpers[roomId]

    fun clear() {
        roomHelpers.clear()
    }

    fun clear(roomId: String) {
        roomHelpers.remove(roomId)
    }
}
