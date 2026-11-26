#!/bin/sh
# 构建 + 签名 +（可选）安装
#
# 需要：
#   ANDROID_JAR   android.jar 路径（比如 $SDK/platforms/android-34/android.jar）
#   BUILD_TOOLS   包含 d8 / aapt2 / zipalign / apksigner 的目录
#
#   sh build.sh              只构建
#   sh build.sh --install    构建并 adb 安装

set -e
cd "$(dirname "$0")"

AJ=${ANDROID_JAR:-/root/toolchain/p34/android-34/android.jar}
BT=${BUILD_TOOLS:-/root/toolchain/bt/android-14}

[ -f "$AJ" ] || { echo "找不到 android.jar：$AJ（用 ANDROID_JAR 指定）" >&2; exit 1; }
[ -d "$BT" ] || { echo "找不到 build-tools：$BT（用 BUILD_TOOLS 指定）" >&2; exit 1; }
[ -f keys.jks ] || { echo "缺少 keys.jks，先按 README 生成签名密钥" >&2; exit 1; }

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

echo "built: ledger.apk ($(ls -lh ledger.apk | awk '{print $5}'))"

if [ "$1" = "--install" ]; then
  adb install -r -i com.miui.packageinstaller ledger.apk
  adb shell am start -n com.dsh.ledger/.MainActivity
fi
