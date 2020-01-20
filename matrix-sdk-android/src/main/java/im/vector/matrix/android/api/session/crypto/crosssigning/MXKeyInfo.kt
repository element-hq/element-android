// /*
// * Copyright 2020 New Vector Ltd
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package im.vector.matrix.android.api.session.crypto.crosssigning
//
// import com.squareup.moshi.Json
// import com.squareup.moshi.JsonClass
// import im.vector.matrix.android.internal.crypto.model.rest.CrossSigningKeyInfo
//
//
// @JsonClass(generateAdapter = true)
// data class MXKeyInfo(
//
//        @Json(name = "public_key")
//        val publicKeyBase64: String,
//        val privateKeyBase64: String?,
//
//        @Json(name = "is_trusted")
//        val isTrusted: Boolean = false,
//
//
//        @Json(name = "usage")
//        val usage: List<String> = ArrayList(),
//
//        /**
//         * The signature of this MXDeviceInfo.
//         * A map from "<userId>" to a map from "<key type>:<Publickey>" to "<signature>"
//         */
//        @Json(name = "signatures")
//        var signatures: Map<String, Map<String, String>>? = null
//
// ) {
//
//    data class Builder(
//            private val publicKeyBase64: String,
//            private val usage: CrossSigningKeyInfo.KeyUsage,
//            private var trusted: Boolean = false,
//            private val signatures: ArrayList<Triple<String, String, String>> = ArrayList()
//    ) {
//
//        fun signature(userId: String, keyUsedToSignBase64: String, base64Signature: String) = apply {
//            signatures.add(Triple(userId, keyUsedToSignBase64, base64Signature))
//        }
//
//        fun trusted(trusted: Boolean) = apply {
//            this.trusted = trusted
//        }
//
//        fun build(): MXKeyInfo {
//
//            val signMap = HashMap<String, HashMap<String, String>>()
//            signatures.forEach { info ->
//                val uMap = signMap[info.first]
//                        ?: HashMap<String, String>().also { signMap[info.first] = it }
//                uMap["ed25519:${info.second}"] = info.third
//            }
//
//            return MXKeyInfo(
//                    publicKeyBase64 = publicKeyBase64,
//                    usage = listOf(usage.value),
//                    isTrusted = trusted,
//                    signatures = signMap
//            )
//        }
//    }
// }
//
