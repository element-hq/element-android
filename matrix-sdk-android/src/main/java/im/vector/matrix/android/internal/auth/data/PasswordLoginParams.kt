package im.vector.matrix.android.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PasswordLoginParams(@Json(name = "identifier") val identifier: Map<String, String>,
                               @Json(name = "password") val password: String,
                               @Json(name = "type") override val type: String,
                               @Json(name = "initial_device_display_name") val deviceDisplayName: String?,
                               @Json(name = "device_id") val deviceId: String?) : LoginParams {


    companion object {

        val IDENTIFIER_KEY_TYPE_USER = "m.id.user"
        val IDENTIFIER_KEY_TYPE_THIRD_PARTY = "m.id.thirdparty"
        val IDENTIFIER_KEY_TYPE_PHONE = "m.id.phone"

        val IDENTIFIER_KEY_TYPE = "type"
        val IDENTIFIER_KEY_MEDIUM = "medium"
        val IDENTIFIER_KEY_ADDRESS = "address"
        val IDENTIFIER_KEY_USER = "user"
        val IDENTIFIER_KEY_COUNTRY = "country"
        val IDENTIFIER_KEY_NUMBER = "number"


        fun userIdentifier(user: String, password: String, deviceDisplayName: String? = null, deviceId: String? = null): PasswordLoginParams {
            val identifier = HashMap<String, String>()
            identifier[IDENTIFIER_KEY_TYPE] = IDENTIFIER_KEY_TYPE_USER
            identifier[IDENTIFIER_KEY_USER] = user
            return PasswordLoginParams(identifier, password, LoginFlowTypes.PASSWORD, deviceDisplayName, deviceId)

        }

        fun thirdPartyIdentifier(medium: String, address: String, password: String, deviceDisplayName: String? = null, deviceId: String? = null): PasswordLoginParams {
            val identifier = HashMap<String, String>()
            identifier[IDENTIFIER_KEY_TYPE] = IDENTIFIER_KEY_TYPE_THIRD_PARTY
            identifier[IDENTIFIER_KEY_MEDIUM] = medium
            identifier[IDENTIFIER_KEY_ADDRESS] = address
            return PasswordLoginParams(identifier, password, LoginFlowTypes.PASSWORD, deviceDisplayName, deviceId)

        }
    }


}