#!/bin/bash

mydir="$(dirname "$(realpath "$0")")"

base_out="$mydir/../vector/src/main/res"

dpi=96

base_folder="$mydir/../vector/src/main/res/drawable"

file="$mydir/riot_splash_0_green.svg"
export_files

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


base_folder="$mydir/../vector/src/main/res/mipmap"
dpi=24 # 96/4

file="$mydir/ic_launcher.svg"
export_files

file="$mydir/ic_launcher_round.svg"
export_files
