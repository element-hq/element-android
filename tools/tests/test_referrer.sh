#!/usr/bin/env bash


adb shell am broadcast -a com.android.vending.INSTALL_REFERRER \
  -n im.vector.alpha/im.vector.receiver.VectorReferrerReceiver \
  --es "referrer" "utm_source=migrator\&utm_medium=web\&utm_content%3Dis%253Dhttps%253A%252F%252Fvector.im%252526hs%253Dhttps%253A%252F%252Ftadhack.vector.im"

# adb shell input text 'https://play.google.com/store/apps/details?id=im.vector.alpha\&referrer=utm_source%3Dgoogle%26utm_medium%3Dcpc%26utm_content%3Dis%253Dhttps%253A%252F%252Fvector.im%252526hs%253Dhttps%253A%252F%252Ftadhack.vector.im%26anid%3Dadmob'
