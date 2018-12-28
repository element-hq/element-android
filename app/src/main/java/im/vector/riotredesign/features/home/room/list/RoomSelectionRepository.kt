package im.vector.riotredesign.features.home.room.list

import android.content.SharedPreferences

private const val SHARED_PREFS_SELECTED_ROOM_KEY = "SHARED_PREFS_SELECTED_ROOM_KEY"

class RoomSelectionRepository(private val sharedPreferences: SharedPreferences) {

    fun lastSelectedRoom(): String? {
        return sharedPreferences.getString(SHARED_PREFS_SELECTED_ROOM_KEY, null)
    }

    fun saveLastSelectedRoom(roomId: String) {
        sharedPreferences.edit()
                .putString(SHARED_PREFS_SELECTED_ROOM_KEY, roomId)
                .apply()
    }

}

