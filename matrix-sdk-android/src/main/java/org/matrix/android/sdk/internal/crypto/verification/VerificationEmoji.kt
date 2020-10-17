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

internal fun getEmojiForCode(code: Int): EmojiRepresentation {
    return when (code % 64) {
        0             -> EmojiRepresentation("🐶", R.string.verification_emoji_dog, R.drawable.ic_verification_dog)
        1             -> EmojiRepresentation("🐱", R.string.verification_emoji_cat, R.drawable.ic_verification_cat)
        2             -> EmojiRepresentation("🦁", R.string.verification_emoji_lion, R.drawable.ic_verification_lion)
        3             -> EmojiRepresentation("🐎", R.string.verification_emoji_horse, R.drawable.ic_verification_horse)
        4             -> EmojiRepresentation("🦄", R.string.verification_emoji_unicorn, R.drawable.ic_verification_unicorn)
        5             -> EmojiRepresentation("🐷", R.string.verification_emoji_pig, R.drawable.ic_verification_pig)
        6             -> EmojiRepresentation("🐘", R.string.verification_emoji_elephant, R.drawable.ic_verification_elephant)
        7             -> EmojiRepresentation("🐰", R.string.verification_emoji_rabbit, R.drawable.ic_verification_rabbit)
        8             -> EmojiRepresentation("🐼", R.string.verification_emoji_panda, R.drawable.ic_verification_panda)
        9             -> EmojiRepresentation("🐓", R.string.verification_emoji_rooster, R.drawable.ic_verification_rooster)
        10            -> EmojiRepresentation("🐧", R.string.verification_emoji_penguin, R.drawable.ic_verification_penguin)
        11            -> EmojiRepresentation("🐢", R.string.verification_emoji_turtle, R.drawable.ic_verification_turtle)
        12            -> EmojiRepresentation("🐟", R.string.verification_emoji_fish, R.drawable.ic_verification_fish)
        13            -> EmojiRepresentation("🐙", R.string.verification_emoji_octopus, R.drawable.ic_verification_octopus)
        14            -> EmojiRepresentation("🦋", R.string.verification_emoji_butterfly, R.drawable.ic_verification_butterfly)
        15            -> EmojiRepresentation("🌷", R.string.verification_emoji_flower, R.drawable.ic_verification_flower)
        16            -> EmojiRepresentation("🌳", R.string.verification_emoji_tree, R.drawable.ic_verification_tree)
        17            -> EmojiRepresentation("🌵", R.string.verification_emoji_cactus, R.drawable.ic_verification_cactus)
        18            -> EmojiRepresentation("🍄", R.string.verification_emoji_mushroom, R.drawable.ic_verification_mushroom)
        19            -> EmojiRepresentation("🌏", R.string.verification_emoji_globe, R.drawable.ic_verification_globe)
        20            -> EmojiRepresentation("🌙", R.string.verification_emoji_moon, R.drawable.ic_verification_moon)
        21            -> EmojiRepresentation("☁️", R.string.verification_emoji_cloud, R.drawable.ic_verification_cloud)
        22            -> EmojiRepresentation("🔥", R.string.verification_emoji_fire, R.drawable.ic_verification_fire)
        23            -> EmojiRepresentation("🍌", R.string.verification_emoji_banana, R.drawable.ic_verification_banana)
        24            -> EmojiRepresentation("🍎", R.string.verification_emoji_apple, R.drawable.ic_verification_apple)
        25            -> EmojiRepresentation("🍓", R.string.verification_emoji_strawberry, R.drawable.ic_verification_strawberry)
        26            -> EmojiRepresentation("🌽", R.string.verification_emoji_corn, R.drawable.ic_verification_corn)
        27            -> EmojiRepresentation("🍕", R.string.verification_emoji_pizza, R.drawable.ic_verification_pizza)
        28            -> EmojiRepresentation("🎂", R.string.verification_emoji_cake, R.drawable.ic_verification_cake)
        29            -> EmojiRepresentation("❤️", R.string.verification_emoji_heart, R.drawable.ic_verification_heart)
        30            -> EmojiRepresentation("🙂", R.string.verification_emoji_smiley, R.drawable.ic_verification_smiley)
        31            -> EmojiRepresentation("🤖", R.string.verification_emoji_robot, R.drawable.ic_verification_robot)
        32            -> EmojiRepresentation("🎩", R.string.verification_emoji_hat, R.drawable.ic_verification_hat)
        33            -> EmojiRepresentation("👓", R.string.verification_emoji_glasses, R.drawable.ic_verification_glasses)
        34            -> EmojiRepresentation("🔧", R.string.verification_emoji_spanner, R.drawable.ic_verification_spanner)
        35            -> EmojiRepresentation("🎅", R.string.verification_emoji_santa, R.drawable.ic_verification_santa)
        36            -> EmojiRepresentation("👍", R.string.verification_emoji_thumbs_up, R.drawable.ic_verification_thumbs_up)
        37            -> EmojiRepresentation("☂️", R.string.verification_emoji_umbrella, R.drawable.ic_verification_umbrella)
        38            -> EmojiRepresentation("⌛", R.string.verification_emoji_hourglass, R.drawable.ic_verification_hourglass)
        39            -> EmojiRepresentation("⏰", R.string.verification_emoji_clock, R.drawable.ic_verification_clock)
        40            -> EmojiRepresentation("🎁", R.string.verification_emoji_gift, R.drawable.ic_verification_gift)
        41            -> EmojiRepresentation("💡", R.string.verification_emoji_light_bulb, R.drawable.ic_verification_light_bulb)
        42            -> EmojiRepresentation("📕", R.string.verification_emoji_book, R.drawable.ic_verification_book)
        43            -> EmojiRepresentation("✏️", R.string.verification_emoji_pencil, R.drawable.ic_verification_pencil)
        44            -> EmojiRepresentation("📎", R.string.verification_emoji_paperclip, R.drawable.ic_verification_paperclip)
        45            -> EmojiRepresentation("✂️", R.string.verification_emoji_scissors, R.drawable.ic_verification_scissors)
        46            -> EmojiRepresentation("🔒", R.string.verification_emoji_lock, R.drawable.ic_verification_lock)
        47            -> EmojiRepresentation("🔑", R.string.verification_emoji_key, R.drawable.ic_verification_key)
        48            -> EmojiRepresentation("🔨", R.string.verification_emoji_hammer, R.drawable.ic_verification_hammer)
        49            -> EmojiRepresentation("☎️", R.string.verification_emoji_telephone, R.drawable.ic_verification_phone)
        50            -> EmojiRepresentation("🏁", R.string.verification_emoji_flag, R.drawable.ic_verification_flag)
        51            -> EmojiRepresentation("🚂", R.string.verification_emoji_train, R.drawable.ic_verification_train)
        52            -> EmojiRepresentation("🚲", R.string.verification_emoji_bicycle, R.drawable.ic_verification_bicycle)
        53            -> EmojiRepresentation("✈️", R.string.verification_emoji_aeroplane, R.drawable.ic_verification_aeroplane)
        54            -> EmojiRepresentation("🚀", R.string.verification_emoji_rocket, R.drawable.ic_verification_rocket)
        55            -> EmojiRepresentation("🏆", R.string.verification_emoji_trophy, R.drawable.ic_verification_trophy)
        56            -> EmojiRepresentation("⚽", R.string.verification_emoji_ball, R.drawable.ic_verification_ball)
        57            -> EmojiRepresentation("🎸", R.string.verification_emoji_guitar, R.drawable.ic_verification_guitar)
        58            -> EmojiRepresentation("🎺", R.string.verification_emoji_trumpet, R.drawable.ic_verification_trumpet)
        59            -> EmojiRepresentation("🔔", R.string.verification_emoji_bell, R.drawable.ic_verification_bell)
        60            -> EmojiRepresentation("⚓", R.string.verification_emoji_anchor, R.drawable.ic_verification_anchor)
        61            -> EmojiRepresentation("🎧", R.string.verification_emoji_headphones, R.drawable.ic_verification_headphones)
        62            -> EmojiRepresentation("📁", R.string.verification_emoji_folder, R.drawable.ic_verification_folder)
        /* 63 */ else -> EmojiRepresentation("📌", R.string.verification_emoji_pin, R.drawable.ic_verification_pin)
    }
}
