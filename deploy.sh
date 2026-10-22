#!/bin/bash
# 一句话部署：本地 JDK21 打包(含前端) → rsync 传 jar → ssh 重启 systemd 服务
# 用法: ./deploy.sh
# 依赖: 本机 openjdk@21；与服务器免密 SSH(~/.ssh/id_ed25519_cordys)；服务器已装 cordys.service
set -euo pipefail

SERVER="root@8.166.129.245"
SSH_KEY="$HOME/.ssh/id_ed25519_cordys"
REMOTE_JAR="/opt/cordys/app-main.jar"
LOCAL_JAR="backend/app/target/app-main.jar"

echo "==> [1/3] 本地打包（JDK21，含前端）"
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./mvnw clean package -DskipTests

echo "==> [2/3] rsync 传 jar -> $SERVER:$REMOTE_JAR"
rsync -az --partial -e "ssh -i $SSH_KEY -o ConnectTimeout=12" "$LOCAL_JAR" "$SERVER:$REMOTE_JAR"

echo "==> [3/3] 重启服务"
ssh -i "$SSH_KEY" -o ConnectTimeout=12 "$SERVER" \
  'systemctl restart cordys && echo "服务已重启，状态: $(systemctl is-active cordys)"'

echo "==> 完成"
