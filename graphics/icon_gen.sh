#!/bin/bash

mydir="$(dirname "$(realpath "$0")")"

base_out="$mydir/../vector/src/main/res"

export_files() {
    newfile="$(basename "$file" .svg).png"
    mkdir -p $base_folder-mdpi
    mkdir -p $base_folder-hdpi
    mkdir -p $base_folder-xhdpi
    mkdir -p $base_folder-xxhdpi
    mkdir -p $base_folder-xxxhdpi
    inkscape "$file" --export-filename="$base_folder-mdpi/$newfile" -C --export-dpi=$dpi
    inkscape "$file" --export-filename="$base_folder-hdpi/$newfile" -C --export-dpi=$(($dpi*3/2))
    inkscape "$file" --export-filename="$base_folder-xhdpi/$newfile" -C --export-dpi=$(($dpi*2))
    inkscape "$file" --export-filename="$base_folder-xxhdpi/$newfile" -C --export-dpi=$(($dpi*3))
    inkscape "$file" --export-filename="$base_folder-xxxhdpi/$newfile" -C --export-dpi=$(($dpi*4))
}

dpi=96


base_folder="$mydir/../vector/src/main/res/drawable"

cp "$mydir/ic_launcher_sc.svg" "$mydir/riot_splash_sc.svg"
file="$mydir/riot_splash_sc.svg"
export_files
rm "$mydir/riot_splash_sc.svg"

file="$mydir/materialdesignicons/ic_status_bar_sc.svg"
export_files

dpi=48
cp "$mydir/ic_launcher_sc.svg" "$mydir/element_logo_sc.svg"
file="$mydir/element_logo_sc.svg"
export_files
rm "$mydir/element_logo_sc.svg"


base_folder="$mydir/../vector/src/main/res/mipmap"
dpi=24 # 96/4

file="$mydir/ic_launcher_sc.svg"
export_files


dpi=48 # 96/2
file="$mydir/ic_launcher_foreground_sc.svg"
export_files


inkscape "$mydir/feature_image.svg" --export-filename="$mydir/../fastlane/metadata/android/en-US/images/featureGraphic.png" -C --export-dpi=96
inkscape "$mydir/store_icon.svg" --export-filename="$mydir/../fastlane/metadata/android/en-US/images/icon.png" -C --export-dpi=96
