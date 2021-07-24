#!/bin/bash

set -e
shopt -s globstar

my_dir="$(dirname "$(realpath "$0")")"
pushd "$my_dir" > /dev/null

function add_alpha_to_color() {
    local color="$1"
    local alpha="$2"
    echo "$color" | sed "s|#|#$alpha|"
}

function add_noalpha_to_color() {
    local color="$1"
    local alpha="$2"
    python -c "print('#' + hex(int(255 - $alpha*(255-int('$color'[1:3], 16))))[2:] + hex(int(255 - $alpha*(255-int('$color'[3:5], 16))))[2:] + hex(int(255 - $alpha*(255-int('$color'[5:7], 16))))[2:])"
}

function theme_file() {
    local file="$1"
    local name="$2"
    local color="$3"
    local name_lc=`echo "$name" | tr '[:upper:]' '[:lower:]'`
    local color_alpha25=`add_alpha_to_color "$color" "3f"`
    local color_noalpha12=`add_noalpha_to_color "$color" '0.12'`
    local target_file=`echo "$file" | sed "s|bluelight|$name_lc|g"`
    if [ "$file" = "$target_file" ]; then
        return
    fi
    cp "$file" "$target_file"
    sed -i "s|BlueLight|$name|g;s|bluelight|$name_lc|g;s|#03a9f4|$color|g;s|#3f03a9f4|$color_alpha25|g;s|#e0f4f3|$color_noalpha12|g" "$target_file"
}

function insert_above_comment() {
    local comment_pre="$1"
    local comment_post="$2"
    local file="$3"
    local comment="$4"
    local insert="$5"
    local indention="$6"
    if ! grep -q "$insert" "$file"; then
        cat "$file" | tr '\n' '\r' | sed "s|\\($comment_pre$comment$comment_post\\)|$insert\r$indention\\1|" | tr '\r' '\n' > "$file.tmp"
        mv "$file.tmp" "$file"
    fi
}

function insert_above_java_comment() {
    insert_above_comment "// " "" "$@"
}

function insert_above_xml_comment() {
    insert_above_comment "<!-- " " -->" "$@"
}

function generate_accent_common() {
    local name="$1"
    local name_str="$2"
    local name_lc=`echo "$name" | tr '[:upper:]' '[:lower:]'`
    insert_above_xml_comment "vector/src/main/res/values/strings_sc.xml" "do not change this comment for accent generation" "<string name=\"sc_accent_$name_lc\">$name_str</string>" "    "
}

function generate_accent_light() {
    # Usage:
    # generate_accent <name> <color_for_light_themes>
    local name="$1"
    local name_str="$2"
    local color_lt="$3"
    local name_lc=`echo "$name" | tr '[:upper:]' '[:lower:]'`

    # String
    generate_accent_common "$name" "$name_str"

    # Settings arrays
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for light accent entry generation" \
        "<item>@string/sc_accent_$name_lc</item> <!-- Light $name name -->" \
        "        "
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for light accent value generation" \
        "<item>$name_lc</item> <!-- Light $name id -->" \
        "        "
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for light accent preview generation" \
        "<item>$color_lt</item> <!-- Light $name accent -->" \
        "        "

    # Actual theming
    for f in **/"theme_sc_light_accent_bluelight.xml"; do
        theme_file "$f" "$name" "$color_lt" \;
    done

    # Selection code
    insert_above_java_comment "vector/src/main/java/im/vector/app/features/themes/ThemeUtils.kt" \
        "Do not change this comment for automatic light theme insertion" \
        "\"$name_lc\" -> R.style.AppTheme_SC_Light_$name" \
        "                    "
}

function generate_accent_dark() {
    # Usage:
    # generate_accent <name> <color_for_dark_themes>
    local name="$1"
    local name_str="$2"
    local color_dk="$3"
    local name_lc=`echo "$name" | tr '[:upper:]' '[:lower:]'`

    # String
    generate_accent_common "$name" "$name_str"

    # Settings arrays
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for dark accent entry generation" \
        "<item>@string/sc_accent_$name_lc</item> <!-- Dark $name name -->" \
        "        "
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for dark accent value generation" \
        "<item>$name_lc</item> <!-- Dark $name id -->" \
        "        "
    insert_above_xml_comment "vector/src/main/res/values/arrays_sc.xml" \
        "do not change this comment for dark accent preview generation" \
        "<item>$color_dk</item> <!-- Dark $name accent -->" \
        "        "

    # Actual theming
    for f in **/"theme_sc_accent_bluelight.xml"; do
        theme_file "$f" "$name" "$color_dk" \;
    done

    # Selection code
    insert_above_java_comment "vector/src/main/java/im/vector/app/features/themes/ThemeUtils.kt" \
        "Do not change this comment for automatic black theme insertion" \
        "\"$name_lc\" -> R.style.AppTheme_SC_$name" \
        "                    "
    insert_above_java_comment "vector/src/main/java/im/vector/app/features/themes/ThemeUtils.kt" \
        "Do not change this comment for automatic dark theme insertion" \
        "\"$name_lc\" -> R.style.AppTheme_SC_Dark_$name" \
        "                    "
    insert_above_java_comment "vector/src/main/java/im/vector/app/features/themes/ThemeUtils.kt" \
        "Do not change this comment for automatic black colored theme insertion" \
        "\"$name_lc\" -> R.style.AppTheme_SC_Colored_$name" \
        "                    "
    insert_above_java_comment "vector/src/main/java/im/vector/app/features/themes/ThemeUtils.kt" \
        "Do not change this comment for automatic dark colored theme insertion" \
        "\"$name_lc\" -> R.style.AppTheme_SC_Dark_Colored_$name" \
        "                    "
}

function generate_accent() {
    # Usage:
    # generate_accent <name> <color_for_light_themes> <color_for_dark_themes>
    local name="$1"
    local color_lt="$2"
    local color_dk="$3"
    local name_str="$4"
    if [ -z "$name_str" ]; then
        name_str="$name"
    fi
    generate_accent_light "$name" "$name_str" "$color_lt"
    generate_accent_dark "$name" "$name_str" "$color_dk"
}

generate_accent "Amber" "#ffa000" "#ffab00"
generate_accent "BlueLight" "#03a9f4" "#03a9f4" "Light blue"
generate_accent "Carnation" "#fb83b2" "#ffa6c9"
generate_accent "Cyan" "#00bcd4" "#00bcd4"
generate_accent "Denim" "#1560BD" "#1560BD"
generate_accent "Gold" "#CFB53B" "#CFB53B"
#generate_accent "GreenLight" "#8bc34a" "#8bc34a" "Light green"
#generate_accent "Grey" "#808080" "#808080"
#generate_accent "Hope" "#5fc72d" "#59ff3a"
generate_accent "Indigo" "#536DFE" "#536DFE"
generate_accent "Lava" "#B20120" "#EB0028"
generate_accent "Lime" "#cddc39" "#cddc39"
generate_accent "Orange" "#ff9800" "#ff9800"
#generate_accent "Oxygen" "#53ADEF" "#53ADEF"
generate_accent "Pink" "#e91e63" "#f48fb1"
#generate_accent "Pixel" "#4285f4" "#3367d6"
generate_accent "Purple" "#673ab7" "#673ab7"
generate_accent "Red" "#ff0000" "#ff0000"
generate_accent "Teal" "#008577" "#80cbc4"
generate_accent "Turquoise" "#00C1C1" "#00FFFF"
generate_accent "Yellow" "#fdd835" "#ffeb3b"

# We have foreground on accent colors, better skip these
#generate_accent "Grey" "#808080" "#808080"
#generate_accent_light "Black" "Black" "#212121"
#generate_accent_dark "White" "White" "#eeeeee"

popd > /dev/null
