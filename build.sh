#!/bin/sh
# 一键构建 + 安装 小账本
set -e
cd "$(dirname "$0")"
AJ=/root/toolchain/p34/android-34/android.jar
BT=/root/toolchain/bt/android-14

rm -rf out compiled base.apk aligned.apk ledger.apk
mkdir -p out compiled

javac -source 8 -target 8 -nowarn -bootclasspath "$AJ" -d out $(find src -name "*.java")
"$BT/d8" --lib "$AJ" --min-api 21 --output out $(find out -name "*.class")
aapt2 compile --dir res -o compiled/
aapt2 link -o base.apk -I "$AJ" --manifest AndroidManifest.xml \
    --min-sdk-version 21 --target-sdk-version 34 compiled/*.flat
(cd out && zip -q ../base.apk classes.dex)
zipalign -f 4 base.apk aligned.apk
apksigner sign --ks keys.jks --ks-pass pass:ledgerpass --key-pass pass:ledgerpass \
    --out ledger.apk aligned.apk

echo "built: $(ls -lh ledger.apk | awk '{print $5}')"

if [ "$1" = "--install" ]; then
  adb install -r -i com.miui.packageinstaller ledger.apk
  adb shell am start -n com.dsh.ledger/.MainActivity
fi
