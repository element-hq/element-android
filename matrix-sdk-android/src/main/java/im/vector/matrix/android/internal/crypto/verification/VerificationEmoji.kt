/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.R
import im.vector.matrix.android.api.session.crypto.sas.EmojiRepresentation

internal fun getEmojiForCode(code: Int): EmojiRepresentation {
    return when (code % 64) {
        0 -> EmojiRepresentation("🐶", R.string.verification_emoji_dog)
        1 -> EmojiRepresentation("🐱", R.string.verification_emoji_cat)
        2 -> EmojiRepresentation("🦁", R.string.verification_emoji_lion)
        3 -> EmojiRepresentation("🐎", R.string.verification_emoji_horse)
        4 -> EmojiRepresentation("🦄", R.string.verification_emoji_unicorn)
        5 -> EmojiRepresentation("🐷", R.string.verification_emoji_pig)
        6 -> EmojiRepresentation("🐘", R.string.verification_emoji_elephant)
        7 -> EmojiRepresentation("🐰", R.string.verification_emoji_rabbit)
        8 -> EmojiRepresentation("🐼", R.string.verification_emoji_panda)
        9 -> EmojiRepresentation("🐓", R.string.verification_emoji_rooster)
        10 -> EmojiRepresentation("🐧", R.string.verification_emoji_penguin)
        11 -> EmojiRepresentation("🐢", R.string.verification_emoji_turtle)
        12 -> EmojiRepresentation("🐟", R.string.verification_emoji_fish)
        13 -> EmojiRepresentation("🐙", R.string.verification_emoji_octopus)
        14 -> EmojiRepresentation("🦋", R.string.verification_emoji_butterfly)
        15 -> EmojiRepresentation("🌷", R.string.verification_emoji_flower)
        16 -> EmojiRepresentation("🌳", R.string.verification_emoji_tree)
        17 -> EmojiRepresentation("🌵", R.string.verification_emoji_cactus)
        18 -> EmojiRepresentation("🍄", R.string.verification_emoji_mushroom)
        19 -> EmojiRepresentation("🌏", R.string.verification_emoji_globe)
        20 -> EmojiRepresentation("🌙", R.string.verification_emoji_moon)
        21 -> EmojiRepresentation("☁️", R.string.verification_emoji_cloud)
        22 -> EmojiRepresentation("🔥", R.string.verification_emoji_fire)
        23 -> EmojiRepresentation("🍌", R.string.verification_emoji_banana)
        24 -> EmojiRepresentation("🍎", R.string.verification_emoji_apple)
        25 -> EmojiRepresentation("🍓", R.string.verification_emoji_strawberry)
        26 -> EmojiRepresentation("🌽", R.string.verification_emoji_corn)
        27 -> EmojiRepresentation("🍕", R.string.verification_emoji_pizza)
        28 -> EmojiRepresentation("🎂", R.string.verification_emoji_cake)
        29 -> EmojiRepresentation("❤️", R.string.verification_emoji_heart)
        30 -> EmojiRepresentation("😀", R.string.verification_emoji_smiley)
        31 -> EmojiRepresentation("🤖", R.string.verification_emoji_robot)
        32 -> EmojiRepresentation("🎩", R.string.verification_emoji_hat)
        33 -> EmojiRepresentation("👓", R.string.verification_emoji_glasses)
        34 -> EmojiRepresentation("🔧", R.string.verification_emoji_wrench)
        35 -> EmojiRepresentation("🎅", R.string.verification_emoji_santa)
        36 -> EmojiRepresentation("👍", R.string.verification_emoji_thumbsup)
        37 -> EmojiRepresentation("☂️", R.string.verification_emoji_umbrella)
        38 -> EmojiRepresentation("⌛", R.string.verification_emoji_hourglass)
        39 -> EmojiRepresentation("⏰", R.string.verification_emoji_clock)
        40 -> EmojiRepresentation("🎁", R.string.verification_emoji_gift)
        41 -> EmojiRepresentation("💡", R.string.verification_emoji_lightbulb)
        42 -> EmojiRepresentation("📕", R.string.verification_emoji_book)
        43 -> EmojiRepresentation("✏️", R.string.verification_emoji_pencil)
        44 -> EmojiRepresentation("📎", R.string.verification_emoji_paperclip)
        45 -> EmojiRepresentation("✂️", R.string.verification_emoji_scissors)
        46 -> EmojiRepresentation("🔒", R.string.verification_emoji_lock)
        47 -> EmojiRepresentation("🔑", R.string.verification_emoji_key)
        48 -> EmojiRepresentation("🔨", R.string.verification_emoji_hammer)
        49 -> EmojiRepresentation("☎️", R.string.verification_emoji_telephone)
        50 -> EmojiRepresentation("🏁", R.string.verification_emoji_flag)
        51 -> EmojiRepresentation("🚂", R.string.verification_emoji_train)
        52 -> EmojiRepresentation("🚲", R.string.verification_emoji_bicycle)
        53 -> EmojiRepresentation("✈️", R.string.verification_emoji_airplane)
        54 -> EmojiRepresentation("🚀", R.string.verification_emoji_rocket)
        55 -> EmojiRepresentation("🏆", R.string.verification_emoji_trophy)
        56 -> EmojiRepresentation("⚽", R.string.verification_emoji_ball)
        57 -> EmojiRepresentation("🎸", R.string.verification_emoji_guitar)
        58 -> EmojiRepresentation("🎺", R.string.verification_emoji_trumpet)
        59 -> EmojiRepresentation("🔔", R.string.verification_emoji_bell)
        60 -> EmojiRepresentation("⚓", R.string.verification_emoji_anchor)
        61 -> EmojiRepresentation("🎧", R.string.verification_emoji_headphone)
        62 -> EmojiRepresentation("📁", R.string.verification_emoji_folder)
        /* 63 */ else -> EmojiRepresentation("📌", R.string.verification_emoji_pin)
    }
}
