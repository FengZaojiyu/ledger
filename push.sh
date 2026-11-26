#!/bin/sh
# 推送到 GitHub（token 从 /root/.dsh/github-token 读，不写进 .git/config）
#
#   sh push.sh            推 main + 所有 tag
#   sh push.sh --release  推完后再建 Release 并上传 dist/*.apk
#
# 注意：本机全局 git 配置里曾经有一条 url.<mirror>.insteadOf 把 github.com
# 重写到只读镜像，推送会 504。这里用 GIT_CONFIG_GLOBAL 绕开全局配置。

set -e
cd "$(dirname "$0")"
TOKEN=$(cat /root/.dsh/github-token 2>/dev/null || echo "")
USER_NAME="FengZaojiyu"
REPO="ledger"
[ -n "$TOKEN" ] || { echo "缺少 token：/root/.dsh/github-token" >&2; exit 1; }

URL="https://${USER_NAME}:${TOKEN}@github.com/${USER_NAME}/${REPO}.git"

echo "→ push main"
GIT_CONFIG_GLOBAL=/dev/null GIT_TERMINAL_PROMPT=0 git push "$URL" main
echo "→ push tags"
GIT_CONFIG_GLOBAL=/dev/null GIT_TERMINAL_PROMPT=0 git push "$URL" --tags
echo "完成：https://github.com/${USER_NAME}/${REPO}"

if [ "$1" = "--release" ]; then
  VER=$(grep -o 'versionName="[^"]*"' AndroidManifest.xml | head -1 | cut -d'"' -f2)
  echo "→ Release v$VER"
  python3 - "$VER" <<'PY'
import json, os, subprocess, sys, glob
ver = sys.argv[1]
token = open("/root/.dsh/github-token").read().strip()
api = "https://api.github.com/repos/FengZaojiyu/ledger"
body = {"tag_name": "v" + ver, "name": "小账本 v" + ver, "draft": False, "prerelease": False,
        "body": "v" + ver}
r = subprocess.run(["curl", "-sS", "-X", "POST", "-H", "Authorization: token " + token,
                    "-H", "Accept: application/vnd.github+json", api + "/releases",
                    "-d", json.dumps(body)], capture_output=True, text=True)
d = json.loads(r.stdout)
rid = d.get("id")
if not rid:
    print("建 Release 失败:", d.get("message")); sys.exit(1)
for f in sorted(glob.glob("dist/*.apk")):
    name = os.path.basename(f)
    up = subprocess.run(["curl", "-sS", "-X", "POST", "-H", "Authorization: token " + token,
                         "-H", "Content-Type: application/vnd.android.package-archive",
                         "--data-binary", "@" + f,
                         "https://uploads.github.com/repos/FengZaojiyu/ledger/releases/%d/assets?name=%s" % (rid, name)],
                        capture_output=True, text=True)
    try:
        print("上传", name, "->", json.loads(up.stdout).get("browser_download_url"))
    except Exception:
        print("上传失败", name, up.stdout[:120])
PY
fi
