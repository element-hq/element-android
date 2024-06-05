/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto

import org.amshove.kluent.internal.assertEquals
import org.junit.Test
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryResponse
import org.matrix.android.sdk.internal.di.MoshiProvider

class KeysQueryResponseTest {

    private val moshi = MoshiProvider.providesMoshi()
    private val keysQueryResponseAdapter = moshi.adapter(KeysQueryResponse::class.java)

    private fun aKwysQueryResponseWithDehydrated(): KeysQueryResponse {
        val rawResponseWithDehydratedDevice = """
            {
                "device_keys": {
                    "@dehydration2:localhost": {
                        "TDHZGMDVNO": {
                            "algorithms": [
                                "m.olm.v1.curve25519-aes-sha2",
                                "m.megolm.v1.aes-sha2"
                            ],
                            "device_id": "TDHZGMDVNO",
                            "keys": {
                                "curve25519:TDHZGMDVNO": "ClMOrHlQJqaqr4oESYyPURwD4BSQxMlEZZk/AnYxVSk",
                                "ed25519:TDHZGMDVNO": "5iZ4zfk0URyIH8YOIWnXmJo41Vn34IixGYphkMdDzik"
                            },
                            "signatures": {
                                "@dehydration2:localhost": {
                                    "ed25519:TDHZGMDVNO": "O6VP+ELiCVAJGHaRdReKga0LGMQahjRnp4znZH7iJO6maZV8aSXnpugSoVsSPRvQ4GBkjX+KXAXU+ODZ0J8MDg",
                                    "ed25519:YZ0EmlbDX+t/m/MB5EWkQLw8cEDg7hX4Zy9699h3hd8": "lG3idYliFGOAe4F/7tENIQ6qI0d41VQKY34BHyVvvWKbv63zDDO5kBTwBeXfUSEeRqyxET3SXLXfB1D8E8LUDg"
                                }
                            },
                            "user_id": "@dehydration2:localhost",
                            "unsigned": {
                                "device_display_name": "localhost:8080: Chrome on macOS"
                            }
                        },
                        "Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ": {
                            "algorithms": [
                                "m.olm.v1.curve25519-aes-sha2",
                                "m.megolm.v1.aes-sha2"
                            ],
                            "dehydrated": true,
                            "device_id": "Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ",
                            "keys": {
                                "curve25519:Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ": "Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ",
                                "ed25519:Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ": "sVY5Xq13sIdhC4We/p5CH69++GsIWRNUhHijtucBirs"
                            },
                            "signatures": {
                                "@dehydration2:localhost": {
                                    "ed25519:Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ": "e2aVrdnD/kor2T0Ok/4SC32MW4WB5JXFSd2wnXV8apxFJBfbdZErANiUbo1Zz/HAasaXM5NBfkr/9gVTdph9BQ",
                                    "ed25519:YZ0EmlbDX+t/m/MB5EWkQLw8cEDg7hX4Zy9699h3hd8": "rVzeE1LbB12XOlckxjRLjt3eq2jVlek6OJ4p08+8g8CMoiJDcw1OVzbJuG/8u6ryarxQF6Yqr4Xu2TqCPBmHDw"
                                }
                            },
                            "user_id": "@dehydration2:localhost",
                            "unsigned": {
                                "device_display_name": "Dehydrated device"
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return keysQueryResponseAdapter.fromJson(rawResponseWithDehydratedDevice)!!
    }

    @Test
    fun `Should parse correctly devices with new dehydrated field`() {
        val aKeysQueryResponse = aKwysQueryResponseWithDehydrated()

        val pojoToJson = keysQueryResponseAdapter.toJson(aKeysQueryResponse)

        val rawAdapter = moshi.adapter(Map::class.java)

        val rawJson = rawAdapter.fromJson(pojoToJson)!!

        val deviceKeys = (rawJson["device_keys"] as Map<*, *>)["@dehydration2:localhost"] as Map<*, *>

        assertEquals(deviceKeys.keys.size, 2)

        val dehydratedDevice = deviceKeys["Y2gISVBZ024gKKAe6Xos44cDbNlO/49YjaOyiqFwjyQ"] as Map<*, *>

        assertEquals(dehydratedDevice["dehydrated"] as? Boolean, true)
    }
}
