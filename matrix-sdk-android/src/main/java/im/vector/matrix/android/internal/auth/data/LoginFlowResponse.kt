package im.vector.matrix.android.internal.auth.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LoginFlowResponse(val flows: List<LoginFlow>)