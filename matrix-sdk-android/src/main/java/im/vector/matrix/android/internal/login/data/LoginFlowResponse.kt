package im.vector.matrix.android.internal.login.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginFlowResponse(val flows: List<LoginFlow>)