#!/bin/sh
# 一键推到 GitHub
#
#   sh push.sh <用户名> <仓库名> <token>
#
# token 需要 repo 权限：https://github.com/settings/tokens
# 注意：token 只在这一条命令里用，不会写进 .git/config。

set -e
USER_NAME="$1"
REPO="${2:-ledger}"
TOKEN="$3"

if [ -z "$USER_NAME" ] || [ -z "$TOKEN" ]; then
  echo "用法: sh push.sh <用户名> <仓库名> <token>" >&2
  exit 1
fi

cd "$(dirname "$0")"
URL="https://${USER_NAME}:${TOKEN}@github.com/${USER_NAME}/${REPO}.git"

echo "→ 推 main"
git push "$URL" main
echo "→ 推 tag v2.1"
git push "$URL" v2.1
echo "完成：https://github.com/${USER_NAME}/${REPO}"
