package im.vector.matrix.android.internal.login.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginFlow(val type: String,
                     val stages: List<String>)