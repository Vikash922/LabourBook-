#!/bin/bash
IMG="/app/applet/file_0000000011d8821181782ef59c6cd7e9.png"
RES_DIR="/app/applet/app/src/main/res"

# Clean up existing mipmap files to avoid conflicts
find $RES_DIR -name "ic_launcher*.webp" -delete
find $RES_DIR -name "ic_launcher*.png" -delete
find $RES_DIR/drawable -name "ic_launcher_foreground.xml" -delete
find $RES_DIR/drawable -name "ic_launcher_background.xml" -delete

mkdir -p $RES_DIR/drawable-nodpi
mkdir -p $RES_DIR/mipmap-nodpi
mkdir -p $RES_DIR/mipmap-anydpi-v26

# Generate a foreground image with padding so it fits in the circle safe zone
# Original is 1254x1254. We scale it to 340x340, and place on a 512x512 transparent (or white) canvas.
convert "$IMG" -resize 340x340 -background none -gravity center -extent 512x512 $RES_DIR/drawable-nodpi/ic_launcher_foreground.webp

# Generate a solid white background drawable
cat << 'XML' > $RES_DIR/drawable/ic_launcher_background.xml
<?xml version="1.0" encoding="utf-8"?>
<color xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#FFFFFF"/>
XML

# Generate legacy icon (for older devices). Use original image (no padding needed for rounded square, but circle might crop. Let's just use original resized)
convert "$IMG" -resize 192x192 $RES_DIR/mipmap-nodpi/ic_launcher.webp
convert "$IMG" -resize 192x192 $RES_DIR/mipmap-nodpi/ic_launcher_round.webp

# Create mipmap-anydpi-v26 XMLs for adaptive icons
cat << 'XML' > $RES_DIR/mipmap-anydpi-v26/ic_launcher.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
XML

cat << 'XML' > $RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
XML
