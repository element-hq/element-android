/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.R
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.internal.extensions.toUnsignedInt

internal fun getEmojiForCode(code: Int): EmojiRepresentation {
    return when (code % 64) {
        0 -> EmojiRepresentation("🐶", R.string.verification_emoji_dog, R.drawable.ic_verification_dog)
        1 -> EmojiRepresentation("🐱", R.string.verification_emoji_cat, R.drawable.ic_verification_cat)
        2 -> EmojiRepresentation("🦁", R.string.verification_emoji_lion, R.drawable.ic_verification_lion)
        3 -> EmojiRepresentation("🐎", R.string.verification_emoji_horse, R.drawable.ic_verification_horse)
        4 -> EmojiRepresentation("🦄", R.string.verification_emoji_unicorn, R.drawable.ic_verification_unicorn)
        5 -> EmojiRepresentation("🐷", R.string.verification_emoji_pig, R.drawable.ic_verification_pig)
        6 -> EmojiRepresentation("🐘", R.string.verification_emoji_elephant, R.drawable.ic_verification_elephant)
        7 -> EmojiRepresentation("🐰", R.string.verification_emoji_rabbit, R.drawable.ic_verification_rabbit)
        8 -> EmojiRepresentation("🐼", R.string.verification_emoji_panda, R.drawable.ic_verification_panda)
        9 -> EmojiRepresentation("🐓", R.string.verification_emoji_rooster, R.drawable.ic_verification_rooster)
        10 -> EmojiRepresentation("🐧", R.string.verification_emoji_penguin, R.drawable.ic_verification_penguin)
        11 -> EmojiRepresentation("🐢", R.string.verification_emoji_turtle, R.drawable.ic_verification_turtle)
        12 -> EmojiRepresentation("🐟", R.string.verification_emoji_fish, R.drawable.ic_verification_fish)
        13 -> EmojiRepresentation("🐙", R.string.verification_emoji_octopus, R.drawable.ic_verification_octopus)
        14 -> EmojiRepresentation("🦋", R.string.verification_emoji_butterfly, R.drawable.ic_verification_butterfly)
        15 -> EmojiRepresentation("🌷", R.string.verification_emoji_flower, R.drawable.ic_verification_flower)
        16 -> EmojiRepresentation("🌳", R.string.verification_emoji_tree, R.drawable.ic_verification_tree)
        17 -> EmojiRepresentation("🌵", R.string.verification_emoji_cactus, R.drawable.ic_verification_cactus)
        18 -> EmojiRepresentation("🍄", R.string.verification_emoji_mushroom, R.drawable.ic_verification_mushroom)
        19 -> EmojiRepresentation("🌏", R.string.verification_emoji_globe, R.drawable.ic_verification_globe)
        20 -> EmojiRepresentation("🌙", R.string.verification_emoji_moon, R.drawable.ic_verification_moon)
        21 -> EmojiRepresentation("☁️", R.string.verification_emoji_cloud, R.drawable.ic_verification_cloud)
        22 -> EmojiRepresentation("🔥", R.string.verification_emoji_fire, R.drawable.ic_verification_fire)
        23 -> EmojiRepresentation("🍌", R.string.verification_emoji_banana, R.drawable.ic_verification_banana)
        24 -> EmojiRepresentation("🍎", R.string.verification_emoji_apple, R.drawable.ic_verification_apple)
        25 -> EmojiRepresentation("🍓", R.string.verification_emoji_strawberry, R.drawable.ic_verification_strawberry)
        26 -> EmojiRepresentation("🌽", R.string.verification_emoji_corn, R.drawable.ic_verification_corn)
        27 -> EmojiRepresentation("🍕", R.string.verification_emoji_pizza, R.drawable.ic_verification_pizza)
        28 -> EmojiRepresentation("🎂", R.string.verification_emoji_cake, R.drawable.ic_verification_cake)
        29 -> EmojiRepresentation("❤️", R.string.verification_emoji_heart, R.drawable.ic_verification_heart)
        30 -> EmojiRepresentation("🙂", R.string.verification_emoji_smiley, R.drawable.ic_verification_smiley)
        31 -> EmojiRepresentation("🤖", R.string.verification_emoji_robot, R.drawable.ic_verification_robot)
        32 -> EmojiRepresentation("🎩", R.string.verification_emoji_hat, R.drawable.ic_verification_hat)
        33 -> EmojiRepresentation("👓", R.string.verification_emoji_glasses, R.drawable.ic_verification_glasses)
        34 -> EmojiRepresentation("🔧", R.string.verification_emoji_spanner, R.drawable.ic_verification_spanner)
        35 -> EmojiRepresentation("🎅", R.string.verification_emoji_santa, R.drawable.ic_verification_santa)
        36 -> EmojiRepresentation("👍", R.string.verification_emoji_thumbs_up, R.drawable.ic_verification_thumbs_up)
        37 -> EmojiRepresentation("☂️", R.string.verification_emoji_umbrella, R.drawable.ic_verification_umbrella)
        38 -> EmojiRepresentation("⌛", R.string.verification_emoji_hourglass, R.drawable.ic_verification_hourglass)
        39 -> EmojiRepresentation("⏰", R.string.verification_emoji_clock, R.drawable.ic_verification_clock)
        40 -> EmojiRepresentation("🎁", R.string.verification_emoji_gift, R.drawable.ic_verification_gift)
        41 -> EmojiRepresentation("💡", R.string.verification_emoji_light_bulb, R.drawable.ic_verification_light_bulb)
        42 -> EmojiRepresentation("📕", R.string.verification_emoji_book, R.drawable.ic_verification_book)
        43 -> EmojiRepresentation("✏️", R.string.verification_emoji_pencil, R.drawable.ic_verification_pencil)
        44 -> EmojiRepresentation("📎", R.string.verification_emoji_paperclip, R.drawable.ic_verification_paperclip)
        45 -> EmojiRepresentation("✂️", R.string.verification_emoji_scissors, R.drawable.ic_verification_scissors)
        46 -> EmojiRepresentation("🔒", R.string.verification_emoji_lock, R.drawable.ic_verification_lock)
        47 -> EmojiRepresentation("🔑", R.string.verification_emoji_key, R.drawable.ic_verification_key)
        48 -> EmojiRepresentation("🔨", R.string.verification_emoji_hammer, R.drawable.ic_verification_hammer)
        49 -> EmojiRepresentation("☎️", R.string.verification_emoji_telephone, R.drawable.ic_verification_phone)
        50 -> EmojiRepresentation("🏁", R.string.verification_emoji_flag, R.drawable.ic_verification_flag)
        51 -> EmojiRepresentation("🚂", R.string.verification_emoji_train, R.drawable.ic_verification_train)
        52 -> EmojiRepresentation("🚲", R.string.verification_emoji_bicycle, R.drawable.ic_verification_bicycle)
        53 -> EmojiRepresentation("✈️", R.string.verification_emoji_aeroplane, R.drawable.ic_verification_aeroplane)
        54 -> EmojiRepresentation("🚀", R.string.verification_emoji_rocket, R.drawable.ic_verification_rocket)
        55 -> EmojiRepresentation("🏆", R.string.verification_emoji_trophy, R.drawable.ic_verification_trophy)
        56 -> EmojiRepresentation("⚽", R.string.verification_emoji_ball, R.drawable.ic_verification_ball)
        57 -> EmojiRepresentation("🎸", R.string.verification_emoji_guitar, R.drawable.ic_verification_guitar)
        58 -> EmojiRepresentation("🎺", R.string.verification_emoji_trumpet, R.drawable.ic_verification_trumpet)
        59 -> EmojiRepresentation("🔔", R.string.verification_emoji_bell, R.drawable.ic_verification_bell)
        60 -> EmojiRepresentation("⚓", R.string.verification_emoji_anchor, R.drawable.ic_verification_anchor)
        61 -> EmojiRepresentation("🎧", R.string.verification_emoji_headphones, R.drawable.ic_verification_headphones)
        62 -> EmojiRepresentation("📁", R.string.verification_emoji_folder, R.drawable.ic_verification_folder)
        /* 63 */ else -> EmojiRepresentation("📌", R.string.verification_emoji_pin, R.drawable.ic_verification_pin)
    }
}

/**
 * decimal: generate five bytes by using HKDF.
 * Take the first 13 bits and convert it to a decimal number (which will be a number between 0 and 8191 inclusive),
 * and add 1000 (resulting in a number between 1000 and 9191 inclusive).
 * Do the same with the second 13 bits, and the third 13 bits, giving three 4-digit numbers.
 * In other words, if the five bytes are B0, B1, B2, B3, and B4, then the first number is (B0 << 5 | B1 >> 3) + 1000,
 * the second number is ((B1 & 0x7) << 10 | B2 << 2 | B3 >> 6) + 1000, and the third number is ((B3 & 0x3f) << 7 | B4 >> 1) + 1000.
 * (This method of converting 13 bits at a time is used to avoid requiring 32-bit clients to do big-number arithmetic,
 * and adding 1000 to the number avoids having clients to worry about properly zero-padding the number when displaying to the user.)
 * The three 4-digit numbers are displayed to the user either with dashes (or another appropriate separator) separating the three numbers,
 * or with the three numbers on separate lines.
 */
fun ByteArray.getDecimalCodeRepresentation(separator: String = " "): String {
    val b0 = this[0].toUnsignedInt() // need unsigned byte
    val b1 = this[1].toUnsignedInt() // need unsigned byte
    val b2 = this[2].toUnsignedInt() // need unsigned byte
    val b3 = this[3].toUnsignedInt() // need unsigned byte
    val b4 = this[4].toUnsignedInt() // need unsigned byte
    // (B0 << 5 | B1 >> 3) + 1000
    val first = (b0.shl(5) or b1.shr(3)) + 1000
    // ((B1 & 0x7) << 10 | B2 << 2 | B3 >> 6) + 1000
    val second = ((b1 and 0x7).shl(10) or b2.shl(2) or b3.shr(6)) + 1000
    // ((B3 & 0x3f) << 7 | B4 >> 1) + 1000
    val third = ((b3 and 0x3f).shl(7) or b4.shr(1)) + 1000
    return listOf(first, second, third).joinToString(separator)
}

/**
 * emoji: generate six bytes by using HKDF.
 * Split the first 42 bits into 7 groups of 6 bits, as one would do when creating a base64 encoding.
 * For each group of 6 bits, look up the emoji from Appendix A corresponding
 * to that number 7 emoji are selected from a list of 64 emoji (see Appendix A)
 */
fun ByteArray.getEmojiCodeRepresentation(): List<EmojiRepresentation> {
    val b0 = this[0].toUnsignedInt()
    val b1 = this[1].toUnsignedInt()
    val b2 = this[2].toUnsignedInt()
    val b3 = this[3].toUnsignedInt()
    val b4 = this[4].toUnsignedInt()
    val b5 = this[5].toUnsignedInt()
    return listOf(
            getEmojiForCode((b0 and 0xFC).shr(2)),
            getEmojiForCode((b0 and 0x3).shl(4) or (b1 and 0xF0).shr(4)),
            getEmojiForCode((b1 and 0xF).shl(2) or (b2 and 0xC0).shr(6)),
            getEmojiForCode((b2 and 0x3F)),
            getEmojiForCode((b3 and 0xFC).shr(2)),
            getEmojiForCode((b3 and 0x3).shl(4) or (b4 and 0xF0).shr(4)),
            getEmojiForCode((b4 and 0xF).shl(2) or (b5 and 0xC0).shr(6))
    )
}
