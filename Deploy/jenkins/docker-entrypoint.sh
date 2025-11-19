#!/usr/bin/env bash
set -e

# 여기서는 root여야 함
if [ "$(id -u)" -ne 0 ]; then
  echo "This entrypoint must start as root." >&2
  exec /usr/local/bin/jenkins.sh
fi

# docker.sock GID 매핑
if [ -S /var/run/docker.sock ]; then
  SOCK_GID=$(stat -c '%g' /var/run/docker.sock || echo 0)
  if [ "$SOCK_GID" != "0" ]; then
    getent group docker >/dev/null 2>&1 || groupadd -g "$SOCK_GID" docker || true
    id -nG jenkins | grep -qw docker || usermod -aG docker jenkins || true
  fi
fi

chown -R jenkins:jenkins /var/jenkins_home || true

# jenkins로 권한 다운그레이드 후 본프로세스 실행
exec gosu jenkins /usr/local/bin/jenkins.sh
